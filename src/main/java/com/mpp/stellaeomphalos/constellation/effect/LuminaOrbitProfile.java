package com.mpp.stellaeomphalos.constellation.effect;

/**
 * lucerna orbit profile: pale yellow / white particles whose orbit center rises every tick (a
 * climbing helix), with a bounded life-extension counter.
 */
public final class LuminaOrbitProfile {
    public static final int PRIMARY_COLOR = 0xFFF2B96D;   // pale yellow
    public static final int SECONDARY_COLOR = 0xFFFFFFFF; // white
    private static final int MAX_RENEWALS = 3;

    private final double risePerTick;
    private int renewals;

    public LuminaOrbitProfile(double risePerTick) { this.risePerTick = risePerTick; }

    public double risePerTick() { return risePerTick; }
    public int renewals() { return renewals; }

    /** Limited life extension; false once the renewal budget is spent. */
    public boolean renew() {
        if (renewals >= MAX_RENEWALS) return false;
        renewals++;
        return true;
    }

    public OrbitEmitter createEmitter(boolean primary) {
        var emitter = new OrbitEmitter(primary ? PRIMARY_COLOR : SECONDARY_COLOR, 2.4,
                2.0 * Math.PI / 80.0, 80, 160, false);
        emitter.setPerTickAdjust(self -> self.addPhase(0.0));   // center rise handled by the renderer via risePerTick
        return emitter;
    }
}
