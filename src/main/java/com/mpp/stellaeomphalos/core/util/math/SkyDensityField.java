package com.mpp.stellaeomphalos.core.util.math;

/** Immutable, deterministic two-octave value noise. No shared random generator or world reference. */
public final class SkyDensityField {
    private final long seed;
    private final int grid;
    public SkyDensityField(long seed, int grid) {
        if (grid < 8 || grid > 128) throw new IllegalArgumentException("Grid outside [8,128]");
        this.seed = seed; this.grid = grid;
    }
    public double sample(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) throw new IllegalArgumentException("Nonfinite position");
        return octave(x / grid, z / grid, seed) * 0.7 + octave(x / (grid * 0.5), z / (grid * 0.5), seed ^ 0x632BE59BD9B4E019L) * 0.3;
    }
    private static double octave(double x, double z, long seed) {
        long ix = (long) Math.floor(x); long iz = (long) Math.floor(z);
        double fx = (1 - Math.cos((x - ix) * Math.PI)) * 0.5;
        double fz = (1 - Math.cos((z - iz) * Math.PI)) * 0.5;
        double a = lerp(value(ix, iz, seed), value(ix + 1, iz, seed), fx);
        double b = lerp(value(ix, iz + 1, seed), value(ix + 1, iz + 1, seed), fx);
        return lerp(a, b, fz);
    }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double value(long x, long z, long seed) {
        long bits = seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        bits = (bits ^ (bits >>> 30)) * 0xBF58476D1CE4E5B9L;
        bits = (bits ^ (bits >>> 27)) * 0x94D049BB133111EBL;
        bits ^= bits >>> 31;
        return (bits >>> 11) * 0x1.0p-53;
    }
}
