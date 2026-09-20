package com.mpp.stellaeomphalos.crafting.altar.recipe;

/** Fixed-point LU arithmetic. Carry is millionths of one LU, never a floating stored balance. */
public final class AsterismEnergy {
    public static final long NETWORK_MULTIPLIER = 200;

    public record Balance(long stored, long carry) {}

    private AsterismEnergy() {}

    public static Balance passive(
            long stored,
            long carry,
            long capacity,
            boolean sky,
            int height,
            double night,
            double noise) {
        long scaled = Math.max(0, stored) * 1000000 + Math.max(0, Math.min(999999, carry));
        scaled = scaled * 95 / 100;
        if (sky && height > 40)
            scaled +=
                    Math.round(
                            160000000
                                    * (0.2 + 0.8 * Math.max(0, Math.min(1, night)))
                                    * (0.6 + 0.4 * Math.max(0, Math.min(1, noise)))
                                    * Math.min(1, (height - 40) / 80.0));
        scaled = Math.min(capacity * 1000000, scaled);
        return new Balance(scaled / 1000000, scaled % 1000000);
    }
}
