package com.mpp.stellaeomphalos.core.util.math;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class GeometryKit {
    private GeometryKit() {}
    public static List<Vec3> circle(Vec3 center, double radius, int count) {
        if (!Double.isFinite(radius) || radius < 0 || count < 1 || count > 65536) throw new IllegalArgumentException("Invalid circle");
        var points = new ArrayList<Vec3>(count);
        for (int i = 0; i < count; i++) { double angle = 2 * Math.PI * i / count; points.add(center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius)); }
        return List.copyOf(points);
    }
    public static long chebyshev(BlockPos a, BlockPos b) {
        return Math.max(Math.abs((long) a.getX() - b.getX()), Math.max(Math.abs((long) a.getY() - b.getY()), Math.abs((long) a.getZ() - b.getZ())));
    }
    public static Vec3 spherical(double radius, double azimuth, double elevation) {
        return new Vec3(radius * Math.cos(elevation) * Math.cos(azimuth), radius * Math.sin(elevation), radius * Math.cos(elevation) * Math.sin(azimuth));
    }
}
