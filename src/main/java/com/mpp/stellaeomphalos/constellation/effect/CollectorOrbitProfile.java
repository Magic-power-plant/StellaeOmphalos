package com.mpp.stellaeomphalos.constellation.effect;

/**
 * Collector crystal orbit profile: palette snapshot of the bound sign color; fancy-graphics gate;
 * {@code canPersist} is always false — the burst plays exactly one round.
 */
public final class CollectorOrbitProfile {
    private final int signColor;
    private final boolean fancyOnly;

    public CollectorOrbitProfile(int signColor, boolean fancyOnly) {
        this.signColor = signColor;
        this.fancyOnly = fancyOnly;
    }

    public int signColor() { return signColor; }
    /** Whether the profile should render only under fancy graphics. */
    public boolean fancyOnly() { return fancyOnly; }

    /** One-round emitter: period == lifetime, never persists. */
    public OrbitEmitter createEmitter() {
        return new OrbitEmitter(signColor, 1.6, 2.0 * Math.PI / 60.0, 60, 60, false);
    }
}
