package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

import org.joml.Vector3d;

import java.util.function.BooleanSupplier;

/** Separates orbital period, logical lifetime and emission capability. */
public final class OrbitTrack extends AbstractEffectTrack {
    @FunctionalInterface
    public interface Emitter {
        void emit(Vector3d position);
    }

    private final OrbitDriver driver;
    private final Emitter emitter;
    private final BooleanSupplier keep;
    private final Vector3d position = new Vector3d();

    public OrbitTrack(OrbitDriver driver, Emitter emitter, BooleanSupplier keep, int lifetime) {
        super(EffectLane.WORLD, lifetime, 5, false);
        this.driver = driver;
        this.emitter = emitter;
        this.keep = keep;
        driver.position(0, position);
    }

    @Override
    protected void advance() {
        if (!keep.getAsBoolean()) {
            expire();
            return;
        }
        driver.position(age, position);
        emitter.emit(position);
    }

    public double distanceSq(double x, double y, double z) {
        return position.distanceSquared(x, y, z);
    }

    public void collect(WorldDraw draw) {}
}
