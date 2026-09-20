package com.mpp.stellaeomphalos.constellation.effect;

import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Orbital particle driver parameters (pure data; rendering belongs to Part-7). Period and lifetime
 * are separate fields — the original implementation reused one {@code maxAge} for both, which was
 * misleading. {@link #tick()} advances the orbit; a persistence callback fires when the emitter
 * would serialize, and a per-tick adjustment hook may mutate the angular phase.
 */
public final class OrbitEmitter {
    private final int color;
    private final double orbitRadius;
    private final double angularSpeed;      // radians per tick
    private final int periodTicks;          // full revolution
    private final int lifetimeTicks;        // total survival; separate from the period
    private final boolean canPersist;
    private int age;
    private double phase;
    private @Nullable Consumer<OrbitEmitter> persistCallback;
    private @Nullable Consumer<OrbitEmitter> perTickAdjust;

    public OrbitEmitter(int color, double orbitRadius, double angularSpeed,
                        int periodTicks, int lifetimeTicks, boolean canPersist) {
        if (periodTicks < 1 || lifetimeTicks < 0) throw new IllegalArgumentException("Invalid orbit timing");
        this.color = color;
        this.orbitRadius = orbitRadius;
        this.angularSpeed = angularSpeed;
        this.periodTicks = periodTicks;
        this.lifetimeTicks = lifetimeTicks;
        this.canPersist = canPersist;
    }

    public int color() { return color; }
    public double orbitRadius() { return orbitRadius; }
    public double angularSpeed() { return angularSpeed; }
    public int periodTicks() { return periodTicks; }
    public int lifetimeTicks() { return lifetimeTicks; }
    public boolean canPersist() { return canPersist; }
    public int age() { return age; }
    public double phase() { return phase; }

    public boolean expired() { return age >= lifetimeTicks; }

    /** Advances one tick: per-tick adjustment hook first, then phase integration. */
    public void tick() {
        if (perTickAdjust != null) perTickAdjust.accept(this);
        age++;
        phase = (phase + angularSpeed) % (2.0 * Math.PI);
    }

    /** Fires the persistence callback when this emitter is allowed to persist. */
    public void persist() {
        if (canPersist && persistCallback != null) persistCallback.accept(this);
    }

    public void setPersistCallback(@Nullable Consumer<OrbitEmitter> callback) { this.persistCallback = callback; }
    public void setPerTickAdjust(@Nullable Consumer<OrbitEmitter> hook) { this.perTickAdjust = hook; }

    /** Nudges the phase (used by profiles that move the orbit center, e.g. Lumina's rising helix). */
    public void addPhase(double delta) { phase = (phase + delta) % (2.0 * Math.PI); }
}
