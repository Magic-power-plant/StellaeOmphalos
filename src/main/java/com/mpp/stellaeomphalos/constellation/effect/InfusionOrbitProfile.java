package com.mpp.stellaeomphalos.constellation.effect;

/**
 * Infusion altar orbit profile parameters (machine orbit visuals; pure data for 《客户端渲染界面与音效》).
 */
public final class InfusionOrbitProfile {
    public static final int COLOR = 0xFFB57EDC;
    public static final int PERIOD_TICKS = 90;
    public static final int LIFETIME_TICKS = 270;

    private InfusionOrbitProfile() {}

    public static OrbitEmitter createEmitter() {
        return new OrbitEmitter(COLOR, 2.6, 2.0 * Math.PI / PERIOD_TICKS, PERIOD_TICKS, LIFETIME_TICKS, true);
    }
}
