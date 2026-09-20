package com.mpp.stellaeomphalos.constellation.effect;

/**
 * armara ritual orbit profile: two-tone (red / violet) rotating sigil energy.
 */
public final class AegisOrbitProfile {
    public static final int RED = 0xFFB03A2E;
    public static final int VIOLET = 0xFF7D3C98;

    private final boolean violetPhase;

    public AegisOrbitProfile(boolean violetPhase) { this.violetPhase = violetPhase; }

    public int color() { return violetPhase ? VIOLET : RED; }

    public OrbitEmitter createEmitter() {
        return new OrbitEmitter(color(), 2.0, 2.0 * Math.PI / 40.0, 40, 200, true);
    }
}
