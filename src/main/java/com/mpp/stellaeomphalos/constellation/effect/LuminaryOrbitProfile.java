package com.mpp.stellaeomphalos.constellation.effect;

/**
 * Luminary (illuminator) orbit profile parameters (machine orbit visuals; pure data for Part-7).
 */
public final class LuminaryOrbitProfile {
    public static final int COLOR = 0xFFF9E27D;
    public static final int PERIOD_TICKS = 70;
    public static final int LIFETIME_TICKS = 210;

    private LuminaryOrbitProfile() {}

    public static OrbitEmitter createEmitter() {
        return new OrbitEmitter(COLOR, 1.8, 2.0 * Math.PI / PERIOD_TICKS, PERIOD_TICKS, LIFETIME_TICKS, true);
    }
}
