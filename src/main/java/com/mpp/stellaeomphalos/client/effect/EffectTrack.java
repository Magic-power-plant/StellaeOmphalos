package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

/** A rendering capability. Lifetime belongs to AbstractEffectTrack. */
public interface EffectTrack {
    EffectLane lane();

    boolean isExpired();

    boolean isMandatory();

    int priority();

    void tick();

    void collect(WorldDraw draw);

    double distanceSq(double x, double y, double z);

    void expire();
}
