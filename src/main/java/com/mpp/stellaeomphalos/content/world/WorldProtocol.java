package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.content.blockentity.rite.RitePedestalBlockEntity;
import com.mpp.stellaeomphalos.content.world.capability.WorldCapabilities;
import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.core.platform.StructureAccess;
import com.mpp.stellaeomphalos.network.*;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.network.toServer.*;
import com.mpp.stellaeomphalos.structure.pattern.*;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;

import java.util.*;

/** All action/query authority lives on the server, with loaded-only and distance checks. */
public final class WorldProtocol {
    private static final class Preview {
        private final ResourceLocation id;
        private final BlockPos origin;
        private final long token, expires;
        private final Map<Long, Integer> sent = new LinkedHashMap<>();
        private boolean initialized;

        private Preview(ResourceLocation id, BlockPos origin, long token, long expires) {
            this.id = id;
            this.origin = origin;
            this.token = token;
            this.expires = expires;
        }

        ResourceLocation id() {
            return id;
        }

        BlockPos origin() {
            return origin;
        }

        long token() {
            return token;
        }

        long expires() {
            return expires;
        }
    }

    private static final Map<UUID, Preview> PREVIEWS = new HashMap<>();

    private static final Map<UUID, Integer> RETROGEN_REPORTS = new HashMap<>();

    public static void watchRetrogen(ServerPlayer player) {
        RETROGEN_REPORTS.put(player.getUUID(), 20);
    }

    private WorldProtocol() {}

    public static void register() {
        var handlers = RuntimeServices.current().handlers();
        RuntimeServices.current().throttlePolicy(com.mpp.stellaeomphalos.network.toServer.PktGatewayTravel.class,58,2,.1);
        handlers.register(com.mpp.stellaeomphalos.network.toServer.PktGatewayTravel.class,
                (player,packet)->GateNetworkService.request(player,packet));
        handlers.register(AstrolabeQueryPayload.class, (p, m) -> query(p, m.targetId()));
        handlers.register(SpringProbePayload.class, WorldProtocol::probe);
        handlers.register(
                RiteInteractPayload.class,
                (p, m) -> {
                    if (p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(m.origin())) > 36
                            || !p.serverLevel().hasChunkAt(m.origin())) return;
                    if (p.serverLevel().getBlockEntity(m.origin())
                            instanceof RitePedestalBlockEntity be)
                        be.interact(
                                p,
                                m.slot() == 0
                                        ? InteractionHand.MAIN_HAND
                                        : InteractionHand.OFF_HAND,
                                m.action());
                });
    }

    private static boolean cooldown(ServerPlayer p, String key, int ticks) {
        var data = p.getPersistentData();
        var saved = data.getCompound("PlayerPersisted");
        long now = p.server.overworld().getGameTime();
        if (saved.getLong(key) > now) return false;
        saved.putLong(key, now + ticks);
        data.put("PlayerPersisted", saved);
        return true;
    }

    public static void query(ServerPlayer player, ResourceLocation target) {
        if (!player.isCreative()
                && !player.getMainHandItem().is(WorldContent.ITEMS.get("astrolabe").get())
                && !player.getOffhandItem().is(WorldContent.ITEMS.get("astrolabe").get())) return;
        int precision =
                Math.max(
                        0,
                        Math.min(2, StructureAccess.progress().astrolabePrecision(player, target)));
        if (!cooldown(
                player, "AstrolabeCooldown", precision == 0 ? 200 : precision == 1 ? 1200 : 6000))
            return;
        var pos = AstrolabeLedger.get(player.serverLevel()).nearest(target, player.position(), 128);
        if (pos.isEmpty()) {
            OmphalosChannel.send(
                    player,
                    new AstrolabeFixPayload(target, 0, 0, 0, precision, 1, Optional.empty()));
            return;
        }
        var p = pos.get();
        double dx = p.getX() - player.getX(),
                dz = p.getZ() - player.getZ(),
                distance = Math.hypot(dx, dz);
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        double step = precision == 0 ? 22.5 : precision == 1 ? 5 : 0.1;
        int quantized = (int) Math.round(Math.round(bearing / step) * step * 10);
        int band =
                precision == 0
                        ? (distance < 256 ? 1 : distance < 1024 ? 2 : 3)
                        : precision == 1 ? (int) (Math.round(distance / 64) * 64) : (int) distance;
        OmphalosChannel.send(
                player,
                new AstrolabeFixPayload(
                        target,
                        quantized,
                        precision == 2
                                ? (int)
                                        (Math.toDegrees(
                                                        Math.atan2(
                                                                p.getY() - player.getY(), distance))
                                                * 10)
                                : 0,
                        band,
                        precision,
                        0,
                        precision == 2 ? Optional.of(p) : Optional.empty()));

    }

    public static void probe(ServerPlayer player, SpringProbePayload p) {
        if (Math.abs((player.blockPosition().getX() >> 4) - p.chunkX()) > 1
                || Math.abs((player.blockPosition().getZ() >> 4) - p.chunkZ()) > 1
                || !cooldown(player, "SpringProbeCooldown", 40)) return;
        var chunk = player.serverLevel().getChunkSource().getChunkNow(p.chunkX(), p.chunkZ());
        if (chunk == null) return;
        chunk.getCapability(WorldCapabilities.SPRING)
                .ifPresent(
                        s -> {
                            boolean present = s.present();
                            int band =
                                    present
                                            ? Math.max(
                                                    1,
                                                    (int)
                                                            Math.ceil(
                                                                    4.0
                                                                            * s.remainingMb()
                                                                            / Math.max(
                                                                                    1,
                                                                                    s
                                                                                            .capacityMb())))
                                            : 0;
                            int temp = present ? WorldBehaviorRegistry.temperature(s.fluidId()) : 3;
                            OmphalosChannel.send(
                                    player,
                                    new SpringInfoPayload(
                                            p.chunkX(), p.chunkZ(), present, temp, band));
                        });
    }

    public static boolean preview(ServerPlayer player, ResourceLocation id, BlockPos origin) {
        if (!StructureAccess.progress().canPreview(player, id)
                || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin)) > 48 * 48
                || !cooldown(player, "PreviewCooldown", 10)
                || BlueprintRegistry.find(id).isEmpty()) return false;
        var previous = PREVIEWS.remove(player.getUUID());
        if (previous != null) OmphalosChannel.send(player, new PreviewEndPayload(previous.id()));
        long token = player.getRandom().nextLong();
        PREVIEWS.put(
                player.getUUID(),
                new Preview(
                        id, origin.immutable(), token, player.serverLevel().getGameTime() + 1200));
        OmphalosChannel.send(player, new PreviewStartPayload(id, origin, 0, 2, 1200, token));
        return true;
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;
        for (var player : level.players()) {
            if (RETROGEN_REPORTS.containsKey(player.getUUID())) {
                var plan = WorldBootstrap.plan(level);
                OmphalosChannel.send(
                        player,
                        new RetrogenStatusPayload(
                                plan.processed(),
                                plan.queued(),
                                plan.skipped(),
                                plan.queued() == 0));
                int remaining = RETROGEN_REPORTS.get(player.getUUID()) - 1;
                if (remaining <= 0 || plan.queued() == 0) RETROGEN_REPORTS.remove(player.getUUID());
                else RETROGEN_REPORTS.put(player.getUUID(), remaining);
            }
            var session = PREVIEWS.get(player.getUUID());
            if (session == null) continue;
            var blueprint = BlueprintRegistry.find(session.id());
            if (blueprint.isEmpty()
                    || level.getGameTime() >= session.expires()
                    || player.distanceToSqr(
                                    net.minecraft.world.phys.Vec3.atCenterOf(session.origin()))
                            > 48 * 48
                    || !StructureAccess.progress().canPreview(player, session.id())) {
                end(player);
                continue;
            }
            var missing = new LinkedHashMap<Long, Integer>();
            for (var entry : blueprint.get().blocks().values()) {
                var p = session.origin().offset(entry.relative());
                if (!level.hasChunkAt(p) || !entry.rule().matches(level.getBlockState(p)))
                    missing.put(entry.relative().asLong(), Block.getId(entry.rule().example()));
            }
            if (missing.isEmpty()) {
                end(player);
                continue;
            }
            var changes = new LinkedHashMap<Long, Integer>();
            session.sent.forEach(
                    (pos, state) -> {
                        if (!missing.containsKey(pos)) changes.put(pos, -1);
                    });
            missing.entrySet().stream()
                    .limit(4096)
                    .forEach(
                            e -> {
                                if (!e.getValue().equals(session.sent.get(e.getKey())))
                                    changes.put(e.getKey(), e.getValue());
                            });
            var entries = changes.entrySet().stream().limit(1024).toList();
            for (int start = 0; start < entries.size(); start += 256) {
                var page = entries.subList(start, Math.min(start + 256, entries.size()));
                OmphalosChannel.send(
                        player,
                        new PreviewDiffPayload(
                                session.id(),
                                session.origin(),
                                page.stream().map(Map.Entry::getKey).toList(),
                                page.stream().map(Map.Entry::getValue).toList(),
                                missing.size(),
                                session.token(),
                                !session.initialized));
                session.initialized = true;
                for (var e : page) {
                    if (e.getValue() < 0) session.sent.remove(e.getKey());
                    else session.sent.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    public static void end(ServerPlayer player) {
        var old = PREVIEWS.remove(player.getUUID());
        if (old != null) OmphalosChannel.send(player, new PreviewEndPayload(old.id()));
    }

    public static void clear() {
        PREVIEWS.clear();
        RETROGEN_REPORTS.clear();
    }
}
