package com.mpp.stellaeomphalos.player.boon.root;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraftforge.event.TickEvent;

/**
 * Vicio root behavior: movement grants experience from vanilla travel statistics, weighted per
 * kind (walk 1.0 / sprint 1.2 / fly 0.4 / elytra 0.8) and capped at {@value #MAX_GAIN} per tick.
 * Deltas come from the vanilla stat counters, so only real movement counts.
 */
public final class SoarRootBoon {

    public static final String SIGN = "vicio";
    public static final double WALK_WEIGHT = 1.0;
    public static final double SPRINT_WEIGHT = 1.2;
    public static final double FLY_WEIGHT = 0.4;
    public static final double AVIATE_WEIGHT = 0.8;
    public static final long MAX_GAIN = 500;

    private static final Map<UUID, long[]> LAST_STATS = new HashMap<>();

    private SoarRootBoon() {}

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!RootExpSupport.active(player, SIGN)) {
            LAST_STATS.remove(player.getUUID());
            return;
        }
        var stats = player.getStats();
        long[] current = {
                stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)),
                stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM)),
                stats.getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM)),
                stats.getValue(Stats.CUSTOM.get(Stats.AVIATE_ONE_CM)) };
        var previous = LAST_STATS.put(player.getUUID(), current);
        if (previous == null) return;
        long gain = weighted(current[0] - previous[0], current[1] - previous[1],
                current[2] - previous[2], current[3] - previous[3]);
        if (gain > 0) RootExpSupport.grant(player, SIGN, gain);
    }

    /** Weighted sum of per-kind travel deltas (centimeters), clamped to the per-tick cap. */
    public static long weighted(long walk, long sprint, long fly, long aviate) {
        double sum = Math.max(0, walk) * WALK_WEIGHT + Math.max(0, sprint) * SPRINT_WEIGHT
                + Math.max(0, fly) * FLY_WEIGHT + Math.max(0, aviate) * AVIATE_WEIGHT;
        return Math.min(MAX_GAIN, Math.round(sum));
    }

    static void clear(UUID player) {
        LAST_STATS.remove(player);
    }

    static void clearAll() {
        LAST_STATS.clear();
    }
}
