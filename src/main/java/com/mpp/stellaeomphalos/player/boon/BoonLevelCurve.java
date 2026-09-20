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
        long total = 0;
        for (int i = 2; i <= level; i++) total += 150L + (long) Math.pow(2.0, (i / 2) + 3);
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
