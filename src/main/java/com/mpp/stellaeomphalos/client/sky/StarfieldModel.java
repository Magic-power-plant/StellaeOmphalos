package com.mpp.stellaeomphalos.client.sky;

/** Stable world-seeded points, grouped into five independently twinkling layers by default. */
public final class StarfieldModel {
    public final float[] points;

    public StarfieldModel(long seed, int layers) {
        points = new float[Math.max(1, Math.min(8, layers)) * 100 * 3];
        var r = new java.util.Random(seed);
        for (int i = 0; i < points.length; i += 3) {
            double y = r.nextDouble() * 2 - 1,
                    a = r.nextDouble() * Math.PI * 2,
                    s = Math.sqrt(1 - y * y);
            points[i] = (float) (Math.cos(a) * s * 90);
            points[i + 1] = (float) (y * 90);
            points[i + 2] = (float) (Math.sin(a) * s * 90);
        }
    }
}
