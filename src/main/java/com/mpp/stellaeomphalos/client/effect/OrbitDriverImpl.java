package com.mpp.stellaeomphalos.client.effect;

public final class OrbitDriverImpl implements OrbitDriver {
    private final org.joml.Vector3d u, v, center;
    private final double radius, period;

    public OrbitDriverImpl(
            org.joml.Vector3d axis, org.joml.Vector3d center, double radius, double period) {
        if (axis.lengthSquared() < 1e-9 || period <= 0 || radius < 0)
            throw new IllegalArgumentException("Invalid orbit");
        var n = new org.joml.Vector3d(axis).normalize();
        this.center = new org.joml.Vector3d(center);
        this.radius = radius;
        this.period = period;
        u =
                new org.joml.Vector3d(Math.abs(n.y) < .9 ? 0 : 1, Math.abs(n.y) < .9 ? 1 : 0, 0)
                        .cross(n)
                        .normalize();
        v = new org.joml.Vector3d(n).cross(u);
    }

    public void position(double tick, org.joml.Vector3d out) {
        double a = tick * Math.PI * 2 / period;
        out.set(center).fma(Math.cos(a) * radius, u).fma(Math.sin(a) * radius, v);
    }
}
