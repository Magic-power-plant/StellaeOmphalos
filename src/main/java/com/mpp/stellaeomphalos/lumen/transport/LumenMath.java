package com.mpp.stellaeomphalos.lumen.transport;

/** Pure lumen arithmetic: no Minecraft references, safe for plain JUnit without a game instance. */
public final class LumenMath {
    /** Implicit-edge link range in blocks (Euclidean); neighbor scans cover the 3x3x3 sections around a node. */
    public static final int LINK_RANGE = 16;
    /** Per-hop loss never reaches 1 so a long chain asymptotically approaches zero instead of going negative. */
    public static final double MAX_HOP_LOSS = 0.95;
    public static final int HEIGHT_LOSS_THRESHOLD = 8;
    public static final double HEIGHT_LOSS_PER_BLOCK = 0.01;
    public static final double NOISE_WEIGHT = 0.3;
    private LumenMath() {}

    /** Three-layer shard index level: floorDiv(y - minBuildHeight, 16); out-of-range heights stay valid (may go negative). */
    public static int sectionY(int y, int minBuildHeight) {
        return Math.floorDiv(y - minBuildHeight, 16);
    }

    /** Per-hop loss: base plus +0.01 for every block of height difference beyond 8, clamped below 1. */
    public static double hopLoss(double lossPerHop, int heightDelta) {
        double extra = heightDelta > HEIGHT_LOSS_THRESHOLD ? HEIGHT_LOSS_PER_BLOCK * (heightDelta - HEIGHT_LOSS_THRESHOLD) : 0;
        return Math.min(Math.max(lossPerHop, 0) + extra, MAX_HOP_LOSS);
    }

    /** Uniform-loss helper: delivered = provided * (1 - lossPerHop)^hops. */
    public static long delivered(long provided, double lossPerHop, int hops) {
        if (hops <= 0) return Math.max(provided, 0);
        return scale(provided, Math.pow(1 - hopLoss(lossPerHop, 0), hops));
    }

    /** Scales an LU amount by a retained factor; result stays within [0, provided]. */
    public static long scale(long provided, double factor) {
        if (provided <= 0 || factor <= 0) return 0;
        return Math.min(provided, Math.round(provided * Math.min(factor, 1)));
    }

    /** Sign distribution to gain mapping; enhanced nodes use the steeper curve. */
    public static double signGain(double distribution, boolean enhanced) {
        return enhanced ? 0.6 + 1.1 * distribution : 0.2 + 0.8 * distribution;
    }

    /**
     * Source output in LU/tick: zero without sky access or an effective sign distribution;
     * otherwise baseOutput x gain(distribution) x (1 + 0.3 x noise) x proximity.
     */
    public static long sourceOutput(long baseOutput, boolean seesSky, double distribution, boolean enhanced, double noiseFactor, double proximityFactor) {
        if (!seesSky || baseOutput <= 0 || distribution <= 0) return 0;
        double value = baseOutput * signGain(distribution, enhanced) * (1 + NOISE_WEIGHT * noiseFactor) * proximityFactor;
        return value <= 0 ? 0 : Math.round(value);
    }

    /** Proximity penalty: sources closer than 16 blocks to their nearest peer are scaled by d/16. */
    public static double proximityPenalty(double nearestSourceDistance) {
        if (nearestSourceDistance >= LINK_RANGE) return 1;
        return Math.max(nearestSourceDistance, 0) / LINK_RANGE;
    }
}
