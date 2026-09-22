package com.mpp.stellaeomphalos.player.boon;

import com.mpp.stellaeomphalos.OmphalosConfig;

/**
 * Boon level curve. The total experience required to reach {@code level} is cumulative:
 * {@code expForLevel(i) = expForLevel(i-1) + 150 + floor(2^((i/2)+3))} (integer division on i).
 * The level cap reads {@code progression.maxBoonLevel} from the server config snapshot.
 */
public final class BoonLevelCurve {

    private BoonLevelCurve() {}

    /** Total experience required to reach {@code level}; level 1 sits at 0. */
    public static long expForLevel(int level) {
        if (level < 1) throw new IllegalArgumentException("Levels start at 1");
        var policy = com.mpp.stellaeomphalos.data.loader.DataBootstrap.TABLES
                .entries(com.mpp.stellaeomphalos.data.loader.DataBootstrap.BOON_XP)
                .get(new net.minecraft.resources.ResourceLocation("stellaeomphalos", "default"));
        var thresholds = policy != null && policy.enabled() ? policy.values() : java.util.List.<Integer>of();
        if (level <= thresholds.size()) return thresholds.get(level - 1);
        long total = thresholds.isEmpty() ? 0 : thresholds.get(thresholds.size() - 1);
        for (int i = Math.max(2, thresholds.size() + 1); i <= level; i++) {
            int exponent = i / 2 + 3;
            if (exponent >= 63) return Long.MAX_VALUE;
            long step = 150L + (1L << exponent);
            if (step < 0 || Long.MAX_VALUE - total < step) return Long.MAX_VALUE;
            total += step;
        }
        return total;
    }

    /** Experience span of one level band: the cost of going from {@code level} to {@code level + 1}. */
    public static long spanOf(int level) {
        return expForLevel(level + 1) - expForLevel(level);
    }

    public static int levelForExp(long exp) {
        return levelForExp(exp, maxLevel());
    }

    /** Highest level whose requirement is met, clamped to [1, maxLevel]. */
    public static int levelForExp(long exp, int maxLevel) {
        if (maxLevel < 1) throw new IllegalArgumentException("Nonpositive level cap");
        int level = 1;
        while (level < maxLevel && expForLevel(level + 1) <= exp) level++;
        return level;
    }

    public static int maxLevel() {
        return OmphalosConfig.SERVER.integer("progression.maxBoonLevel");
    }
}
