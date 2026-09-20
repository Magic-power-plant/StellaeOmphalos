package com.mpp.stellaeomphalos.constellation.effect;

/**
 * Resonance altar orbit profile parameters (machine orbit visuals; pure data for Part-7).
 */
public final class ResonanceOrbitProfile {
    public static final int COLOR = 0xFF3FD8CE;
    public static final int PERIOD_TICKS = 100;
    public static final int LIFETIME_TICKS = 300;

    private ResonanceOrbitProfile() {}

    public static OrbitEmitter createEmitter() {
        return new OrbitEmitter(COLOR, 3.0, 2.0 * Math.PI / PERIOD_TICKS, PERIOD_TICKS, LIFETIME_TICKS, true);
    }
}
