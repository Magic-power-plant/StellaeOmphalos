package com.mpp.stellaeomphalos.core.util.math;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class QuatKit {
    private QuatKit() {}
    public static Vec3 rotate(Vec3 vector, Vec3 axis, double radians) {
        if (axis.lengthSqr() < 1.0e-20) throw new IllegalArgumentException("Zero rotation axis");
        var normal = axis.normalize();
        var value = new Quaterniond().rotationAxis(radians, normal.x, normal.y, normal.z).transform(new Vector3d(vector.x, vector.y, vector.z));
        return new Vec3(value.x, value.y, value.z);
    }
}
