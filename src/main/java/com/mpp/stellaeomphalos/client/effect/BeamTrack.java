package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes.Kind;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

public final class BeamTrack extends AbstractEffectTrack {
    private final double x, y, z, ex, ey, ez, width;
    private final int color;

    public BeamTrack(
            double x,
            double y,
            double z,
            double ex,
            double ey,
            double ez,
            double width,
            int color,
            int life) {
        super(EffectLane.WORLD, life, 20, false);
        this.x = x;
        this.y = y;
        this.z = z;
        this.ex = ex;
        this.ey = ey;
        this.ez = ez;
        this.width = width;
        this.color = color;
    }

    @Override
    public double distanceSq(double a, double b, double c) {
        double dx = (x + ex) / 2 - a, dy = (y + ey) / 2 - b, dz = (z + ez) / 2 - c;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public void collect(WorldDraw d) {
        d.beam(d.buffer(Kind.BEAM_ADDITIVE), x, y, z, ex, ey, ez, width, color);
    }
}
