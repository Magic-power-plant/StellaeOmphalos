package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes.Kind;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

public class QuadTrack extends AbstractEffectTrack {
    protected final double x, y, z, size;
    protected final int color;

    public QuadTrack(
            EffectLane lane,
            double x,
            double y,
            double z,
            double size,
            int color,
            int life,
            boolean mandatory) {
        super(lane, life, mandatory ? 100 : 10, mandatory);
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.color = color;
    }

    @Override
    public double distanceSq(double a, double b, double c) {
        return (x - a) * (x - a) + (y - b) * (y - b) + (z - c) * (z - c);
    }

    @Override
    public void collect(WorldDraw d) {
        float fade = remaining(d.partial);
        int c = ((int) ((color >>> 24) * fade) << 24) | (color & 0xffffff);
        double r = size * (1 + (1 - fade) * 2);
        var v = d.buffer(Kind.SOFT_PARTICLE);
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16, b = (i + 1) * Math.PI / 16;
            d.beam(
                    v,
                    x + Math.cos(a) * r,
                    y,
                    z + Math.sin(a) * r,
                    x + Math.cos(b) * r,
                    y,
                    z + Math.sin(b) * r,
                    .014,
                    c);
        }
    }
}
