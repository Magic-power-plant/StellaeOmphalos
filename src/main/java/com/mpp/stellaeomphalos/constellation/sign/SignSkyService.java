package com.mpp.stellaeomphalos.constellation.sign;

import com.mpp.stellaeomphalos.network.OmphalosChannel;
import com.mpp.stellaeomphalos.network.toClient.PktActiveSigns;
import com.mpp.stellaeomphalos.network.toClient.PktSignRename;
import com.mpp.stellaeomphalos.network.toClient.PktSignSkyLayout;
import com.mpp.stellaeomphalos.network.toClient.PktSkySeed;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * Server-authoritative static facade for sky queries. Per-dimension schedulers are derived from the
 * world seed; the client must consume {@code client/sign/SignSkyMirror} instead. Querying a
 * non-server level yields neutral defaults. Time rewinds rebuild the target day directly, which is
 * equivalent to replaying from currentDay+1 because the daily state is a pure function of the day.
 */
public final class SignSkyService {
    private static final Map<ResourceKey<Level>, SignSkyScheduler> SCHEDULERS = new HashMap<>();
    private static final SkySeedSession SESSIONS = new SkySeedSession();

    private SignSkyService() {}

    public static boolean isNight(Level level) { return level.isNight(); }

    public static float distribution(Level level, Sign sign) {
        var scheduler = schedulerFor(level);
        return scheduler == null ? 0.0F : scheduler.distribution(sign);
    }

    public static List<Sign> activeSigns(Level level) {
        var scheduler = schedulerFor(level);
        return scheduler == null ? List.of() : scheduler.activeSigns();
    }

    public static Optional<CelestialOmen> omenNow(Level level) {
        var scheduler = schedulerFor(level);
        if (scheduler == null) return Optional.empty();
        return scheduler.omenAt(dayOf(level), timeOf(level));
    }

    /** Daytime distribution factor in [0,1]: 1 at noon, 0 at midnight; feeds the MeteorStrike damage formula. */
    public static double dayDistributionFactor(Level level) {
        double time = Math.floorMod(level.getDayTime(), SignSkyScheduler.DEFAULT_DAY_LENGTH);
        return 0.5 + 0.5 * Math.cos((time - SignSkyScheduler.DEFAULT_DAY_LENGTH / 4.0)
                / SignSkyScheduler.DEFAULT_DAY_LENGTH * 2.0 * Math.PI);
    }

    /** Uniform pick among the strongest active signs passing the filter; null when none qualify. */
    public static @Nullable Sign randomStrongest(Level level, Random random, Predicate<Sign> filter) {
        var scheduler = schedulerFor(level);
        return scheduler == null ? null : scheduler.randomStrongest(random, filter);
    }

    public static int sessionId(ServerPlayer player) { return SESSIONS.current(player.getUUID()); }

    static synchronized SkySeedSession sessions() { return SESSIONS; }

    static synchronized @Nullable SignSkyScheduler scheduler(ServerLevel level) { return SCHEDULERS.get(level.dimension()); }

    private static synchronized @Nullable SignSkyScheduler schedulerFor(Level level) {
        if (!(level instanceof ServerLevel serverLevel) || !visibleDimension(level)) return null;
        return ensureDay(serverLevel, schedulerOf(serverLevel));
    }

    public static boolean visibleDimension(Level level) {
        var tables = com.mpp.stellaeomphalos.data.loader.DataBootstrap.TABLES;
        var visible = tables.entries(com.mpp.stellaeomphalos.data.loader.DataBootstrap.VISIBLE_DIMENSIONS);
        var suppressed = tables.entries(com.mpp.stellaeomphalos.data.loader.DataBootstrap.SUPPRESSED_DIMENSIONS);
        var id = level.dimension().location();
        return (visible.isEmpty() || visible.values().stream().anyMatch(p -> p.enabled() && p.targets().contains(id)))
                && suppressed.values().stream().noneMatch(p -> p.enabled() && p.targets().contains(id));
    }

    private static SignSkyScheduler schedulerOf(ServerLevel level) {
        return SCHEDULERS.computeIfAbsent(level.dimension(), dim -> {
            var distributed = new ArrayList<Sign>();
            distributed.addAll(SignRegistry.majorSigns());
            distributed.addAll(SignRegistry.ritualSigns());
            return new SignSkyScheduler(level.getSeed(), distributed,
                    SignRegistry.traitSigns().stream().map(TraitSign.class::cast).toList(),
                    SignRegistry.anomalousSigns().stream().map(AnomalousSign.class::cast).toList());
        });
    }

    private static SignSkyScheduler ensureDay(ServerLevel level, SignSkyScheduler scheduler) {
        long day = dayOf(level);
        if (scheduler.day() != day || scheduler.dayTime() != timeOf(level))
            scheduler.setDay(day, level.getDayTime());
        return scheduler;
    }

    private static long dayOf(Level level) {
        return Math.floorDiv(level.getDayTime(), SignSkyScheduler.DEFAULT_DAY_LENGTH);
    }

    private static long timeOf(Level level) {
        return Math.floorMod(level.getDayTime(), SignSkyScheduler.DEFAULT_DAY_LENGTH);
    }

    // ---- session plumbing, subscribed by SignBootstrap on the Forge bus ----

    static synchronized void tick(ServerLevel level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            if (SCHEDULERS.containsKey(level.dimension())) return;   // daylight frozen: keep current day
            ensureDay(level, schedulerOf(level));
            return;
        }
        long previous = schedulerOf(level).day();
        var scheduler = ensureDay(level, schedulerOf(level));
        if (scheduler.day() != previous) broadcastActive(level, scheduler);
    }

    static synchronized void join(ServerPlayer player) { resync(player); }
    static synchronized void dimensionChange(ServerPlayer player) { resync(player); }

    private static void resync(ServerPlayer player) {
        int session = SESSIONS.bump(player.getUUID());
        var level = player.serverLevel();
        var scheduler = ensureDay(level, schedulerOf(level));
        OmphalosChannel.send(player, new PktSkySeed(session, level.dimension(), scheduler.skySeed()));
        sendLayout(player, session, level, scheduler);
        sendActive(player, session, level, scheduler);
    }

    static synchronized void logout(ServerPlayer player) { SESSIONS.remove(player.getUUID()); }

    static synchronized void serverStop() {
        SCHEDULERS.clear();
        SESSIONS.clear();
    }

    /** After a datapack reload rebuilt the registry: drop cached tables, re-push layout/active/names. */
    static synchronized void dataReloaded(@Nullable MinecraftServer server) {
        SCHEDULERS.clear();
        if (server == null) return;
        for (var player : server.getPlayerList().getPlayers()) {
            resync(player);
        }
        broadcastRenames(server);
    }

    private static void broadcastActive(ServerLevel level, SignSkyScheduler scheduler) {
        for (var player : level.players()) sendActive(player, SESSIONS.current(player.getUUID()), level, scheduler);
    }

    private static void sendActive(ServerPlayer player, int session, ServerLevel level, SignSkyScheduler scheduler) {
        var entries = (visibleDimension(level) ? scheduler.activeSigns() : java.util.List.<Sign>of()).stream()
                .map(sign -> new PktActiveSigns.Entry(SignRegistry.numericId(sign), scheduler.distribution(sign)))
                .toList();
        OmphalosChannel.send(player, new PktActiveSigns(session, level.dimension(), (int) scheduler.day(), entries));
    }

    private static void sendLayout(ServerPlayer player, int session, ServerLevel level, SignSkyScheduler scheduler) {
        var sorted = (visibleDimension(level) ? scheduler.activeSigns() : java.util.List.<Sign>of()).stream()
                .sorted(Comparator.comparing(sign -> sign instanceof MajorSign ? 0 : 1))
                .toList();
        var layout = SignSkyAnchorTable.layout(sorted);

        var entries = layout.entrySet().stream()
                .map(entry -> new PktSignSkyLayout.Entry(entry.getKey().id(), entry.getValue().asList()))
                .toList();
        OmphalosChannel.send(player, new PktSignSkyLayout(session, level.dimension(), entries));
    }

    /** Server-side generated names (en_us lexicon) as an authoritative override channel. */
    private static void broadcastRenames(MinecraftServer server) {
        long seed = server.overworld().getSeed();
        var entries = SignRegistry.all().stream()
                .map(sign -> new PktSignRename.Entry(SignRegistry.numericId(sign),
                        net.minecraft.network.chat.Component.literal(
                                SignNamingProvider.generateName("en_us", seed ^ sign.id().hashCode() * 0x9E3779B97F4A7C15L))))
                .toList();
        if (entries.isEmpty()) return;
        for (var player : server.getPlayerList().getPlayers())
            OmphalosChannel.send(player, new PktSignRename(SESSIONS.current(player.getUUID()), entries));
    }
}
