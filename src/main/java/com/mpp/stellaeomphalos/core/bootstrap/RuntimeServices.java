package com.mpp.stellaeomphalos.core.bootstrap;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.core.util.io.AsyncIoExecutor;
import com.mpp.stellaeomphalos.core.util.tick.ServerTickScheduler;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import com.mpp.stellaeomphalos.network.*;
import com.mpp.stellaeomphalos.network.sync.SyncDispatcher;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.network.toServer.*;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Owns all mutable server-session services, including integrated-server restart cleanup. */
public final class RuntimeServices implements AutoCloseable {
    private static RuntimeServices current;
    private final MinecraftServer server;
    private final ServerTickScheduler scheduler =
            new ServerTickScheduler(
                    error -> LogUtils.getLogger().error("Scheduled task failed", error));
    private final AsyncIoExecutor io = new AsyncIoExecutor();
    private final SyncDispatcher sync = new SyncDispatcher();
    private final NetworkThrottle throttle = new NetworkThrottle();

    private record ThrottlePolicy(int burst, double refill) {}

    private final Map<Integer, ThrottlePolicy> throttlePolicies = new HashMap<>();

    public void throttlePolicy(
            Class<? extends OmphalosPayload> type, int id, int burst, double refill) {
        if (OmphalosChannel.PAYLOADS.type(id).type() != type || burst < 1 || refill <= 0)
            throw new IllegalArgumentException("Invalid throttle policy");
        throttlePolicies.put(id, new ThrottlePolicy(burst, refill));
    }

    private final PayloadHandlers<ServerPlayer> handlers = new PayloadHandlers<>();
    private final Map<UUID, Long> debugSessions = new HashMap<>();
    private final Map<
                    net.minecraft.resources.ResourceLocation,
                    com.mpp.stellaeomphalos.core.platform.api.NetworkDiagnosticSource>
            diagnosticSources = new HashMap<>();
    private final Map<String, java.util.List<String>> migrationReports = new HashMap<>();
    private final Map<UUID, Long> awaitingReady = new HashMap<>();
    private final Set<UUID> ready = new HashSet<>();
    private int configRevision = -1;

    private RuntimeServices(MinecraftServer server) {
        this.server = server;
    }

    public static RuntimeServices current() {
        if (current == null) throw new IllegalStateException("No active server session");
        if (!current.server.isSameThread())
            throw new IllegalStateException("Server service accessed off-thread");
        return current;
    }

    public ServerTickScheduler scheduler() {
        return scheduler;
    }

    public SyncDispatcher sync() {
        return sync;
    }

    public AsyncIoExecutor io() {
        return io;
    }

    public PayloadHandlers<ServerPlayer> handlers() {
        return handlers;
    }

    public void registerDiagnosticSource(
            net.minecraft.resources.ResourceLocation id,
            com.mpp.stellaeomphalos.core.platform.api.NetworkDiagnosticSource source) {
        if (diagnosticSources.putIfAbsent(id, source) != null)
            throw new IllegalArgumentException("Duplicate diagnostic source");
    }

    public void recordMigrationReport(String id, java.util.List<String> issues) {
        var report = new PktMigrationReport(id, issues);
        migrationReports.put(report.reportId(), report.issues());
    }

    public static void attach() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(RuntimeServices::start);
        bus.addListener(RuntimeServices::stop);
        bus.addListener(RuntimeServices::tick);
        bus.addListener(RuntimeServices::login);
        bus.addListener(RuntimeServices::logout);
        bus.addListener(RuntimeServices::dimension);
        bus.addListener(RuntimeServices::respawn);
        bus.addListener(RuntimeServices::commands);
        OmphalosChannel.setServerReceiver((player, payload) -> current().receive(player, payload));
    }

    private static void start(ServerAboutToStartEvent event) {
        if (current != null) current.close();
        current = new RuntimeServices(event.getServer());
    }

    private static void stop(ServerStoppingEvent event) {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    private static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || current == null) return;
        var services = current();
        services.scheduler.advance(
                OmphalosConfig.COMMON.integer("performance.maxScheduledTasksPerTick"));
        OmphalosChannel.tickServer();
        services.awaitingReady.values().removeIf(deadline -> System.nanoTime() >= deadline);
        services.debugSessions
                .values()
                .removeIf(deadline -> services.scheduler.currentTick() >= deadline);
        services.sync.flush(
                services.server.getPlayerList().getPlayers().stream()
                        .filter(player -> services.ready.contains(player.getUUID()))
                        .toList());
        int revision = OmphalosConfig.serverRevision();
        if (revision != services.configRevision) {
            services.configRevision = revision;
            services.server
                    .getPlayerList()
                    .getPlayers()
                    .forEach(
                            player -> OmphalosChannel.send(player, new PktConfigVersion(revision)));
        }
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) current().synchronize(player);
    }

    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) current().synchronize(player);
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) current().synchronize(player);
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || current == null) return;
        UUID id = player.getUUID();
        current.awaitingReady.remove(id);
        current.ready.remove(id);
        current.throttle.remove(id);
        current.sync.remove(id);
        current.debugSessions.remove(id);
        OmphalosChannel.disconnectServerPlayer(id);
    }

    private void synchronize(ServerPlayer player) {
        sync.remove(player.getUUID());
        ready.remove(player.getUUID());
        awaitingReady.put(player.getUUID(), System.nanoTime() + 3000000000L);
        OmphalosChannel.send(player, new PktSyncGateClose(PktSyncGateClose.Reason.RESYNC));
        sync.synchronize(player);
        OmphalosChannel.send(player, new PktConfigVersion(OmphalosConfig.serverRevision()));
        OmphalosChannel.send(player, new PktSyncGateOpen(ProtocolVersion.CURRENT.toString()));
        migrationReports.forEach(
                (id, issues) -> OmphalosChannel.send(player, new PktMigrationReport(id, issues)));
    }

    private void receive(ServerPlayer player, OmphalosPayload payload) {
        int id = OmphalosChannel.PAYLOADS.type(payload).id();
        var policy = throttlePolicies.getOrDefault(id, new ThrottlePolicy(3, 0.01));
        var decision =
                throttle.acquire(
                        player.getUUID(),
                        id,
                        scheduler.currentTick(),
                        policy.burst(),
                        policy.refill());
        if (!decision.accepted()) {
            if (decision.warn()) {
                LogUtils.getLogger().warn("Throttled payload {} from {}", id, player.getUUID());
                OmphalosChannel.send(player, new PktThrottleNotice(id, decision.dropped()));
            }
            return;
        }
        if (payload instanceof PktClientSyncReady packet) {
            Long deadline = awaitingReady.remove(player.getUUID());
            if (deadline != null && System.nanoTime() < deadline) {
                OmphalosChannel.acceptPeerVersion(
                        player.getUUID(), ProtocolVersion.parse(packet.protocol()));
                ready.add(player.getUUID());
                sync.synchronize(player);
            }
        } else if (payload instanceof PktDatasetRequest request
                && ready.contains(player.getUUID())) {
            sync.request(player, request.dataset());
        } else if (payload instanceof PktNetworkDebugDumpRequest request
                && ready.contains(player.getUUID())) {
            debug(player, request.position());
        } else if (ready.contains(player.getUUID())) {
            if (!handlers.dispatch(player, payload))
                throw new IllegalArgumentException("No server payload handler");
        }
    }

    private void debug(ServerPlayer player, net.minecraft.core.BlockPos position) {
        if (!player.hasPermissions(2)
                || debugSessions.getOrDefault(player.getUUID(), -1L) <= scheduler.currentTick()
                || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(position)) > 4096
                || !player.serverLevel().hasChunkAt(position)) return;
        var result = new net.minecraft.nbt.CompoundTag();
        result.putString("Dimension", player.serverLevel().dimension().location().toString());
        result.putLong("ServerTick", scheduler.currentTick());
        var reports = new net.minecraft.nbt.ListTag();
        diagnosticSources.forEach(
                (id, source) ->
                        source.inspect(player, position)
                                .ifPresent(
                                        data -> {
                                            var entry = new net.minecraft.nbt.CompoundTag();
                                            entry.putString("Source", id.toString());
                                            entry.put("Data", data.copy());
                                            reports.add(entry);
                                        }));
        result.put("Sources", reports);
        OmphalosChannel.send(player, new PktNetworkDebugDump(position, result));
    }

    private static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher()
                .register(
                        net.minecraft.commands.Commands.literal(
                                        com.mpp.stellaeomphalos.Omphalos.MODID)
                                .requires(source -> source.hasPermission(2))
                                .then(
                                        net.minecraft.commands.Commands.literal("debug")
                                                .then(
                                                        net.minecraft.commands.Commands.literal(
                                                                        "network")
                                                                .executes(
                                                                        context -> {
                                                                            var player =
                                                                                    context.getSource()
                                                                                            .getPlayerOrException();
                                                                            current()
                                                                                    .debugSessions
                                                                                    .put(
                                                                                            player
                                                                                                    .getUUID(),
                                                                                            current()
                                                                                                            .scheduler
                                                                                                            .currentTick()
                                                                                                    + 400);
                                                                            var data =
                                                                                    new net
                                                                                            .minecraft
                                                                                            .nbt
                                                                                            .CompoundTag();
                                                                            data.putBoolean(
                                                                                    "SessionStarted",
                                                                                    true);
                                                                            OmphalosChannel.send(
                                                                                    player,
                                                                                    new PktNetworkDebugDump(
                                                                                            player
                                                                                                    .blockPosition(),
                                                                                            data));
                                                                            context.getSource()
                                                                                    .sendSuccess(
                                                                                            () ->
                                                                                                    net
                                                                                                            .minecraft
                                                                                                            .network
                                                                                                            .chat
                                                                                                            .Component
                                                                                                            .translatable(
                                                                                                                    "stellaeomphalos.command.debug_enabled"),
                                                                                            false);
                                                                            return 1;
                                                                        }))));
    }

    @Override
    public void close() {
        scheduler.close();
        io.close();
        sync.clear();
        throttle.clear();
        throttlePolicies.clear();
        awaitingReady.clear();
        ready.clear();
        OmphalosChannel.resetServer();
        DataBootstrap.TABLES.clearSession();
        debugSessions.clear();
        diagnosticSources.clear();
        migrationReports.clear();
    }
}
