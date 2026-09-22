package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes.Kind;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

/** Deterministic, precomputed trunk and forks; render never recurses or allocates. */
public final class ArcTrack extends AbstractEffectTrack {
    private final double[] points = new double[33 * 3];
    private final int color;

    public ArcTrack(
            long seed, double x, double y, double z, double ex, double ey, double ez, int color) {
        super(EffectLane.WORLD, 16, 30, false);
        this.color = color;
        var r = new java.util.Random(seed);
        for (int i = 0; i <= 32; i++) {
            double t = i / 32.0, j = Math.sin(t * Math.PI) * .7;
            points[i * 3] = x + (ex - x) * t + (r.nextDouble() - .5) * j;
            points[i * 3 + 1] = y + (ey - y) * t + (r.nextDouble() - .5) * j;
            points[i * 3 + 2] = z + (ez - z) * t + (r.nextDouble() - .5) * j;
        }
    }

    @Override
    public double distanceSq(double x, double y, double z) {
        double a = points[48] - x, b = points[49] - y, c = points[50] - z;
        return a * a + b * b + c * c;
    }

    @Override
    public void collect(WorldDraw d) {
        var v = d.buffer(Kind.BEAM_ADDITIVE);
        int end = Math.min(32, (age + 1) * 5);
        int c = (Math.min(140, (int) (remaining(d.partial) * 255)) << 24) | (color & 0xffffff);
        for (int i = 1; i <= end; i++) {
            int p = i * 3;
            d.beam(
                    v,
                    points[p - 3],
                    points[p - 2],
                    points[p - 1],
                    points[p],
                    points[p + 1],
                    points[p + 2],
                    .025,
                    c);
            if (i % 7 == 0)
                d.beam(
                        v,
                        points[p],
                        points[p + 1],
                        points[p + 2],
                        points[p] + .4,
                        points[p + 1] + .7,
                        points[p + 2] - .3,
                        .012,
                        c);
        }
    }
}
