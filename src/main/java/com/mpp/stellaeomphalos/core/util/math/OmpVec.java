package com.mpp.stellaeomphalos.core.util.math;

import net.minecraft.world.phys.Vec3;

/** Caller-owned scratch vector. Never share between threads or use as a value key. */
public final class OmpVec {
    private double x; private double y; private double z;
    public OmpVec set(double x, double y, double z) { this.x = x; this.y = y; this.z = z; return this; }
    public OmpVec normalize() {
        double length = Math.sqrt(x * x + y * y + z * z);
        return length < 1.0e-12 ? set(0, 0, 0) : set(x / length, y / length, z / length);
    }
    public OmpVec scale(double scalar) { return set(x * scalar, y * scalar, z * scalar); }
    public Vec3 immutable() { return new Vec3(x, y, z); }
}
