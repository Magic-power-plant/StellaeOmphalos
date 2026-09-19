package com.mpp.stellaeomphalos.core.util.world;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SightlineTracer {
    private SightlineTracer() {}
    public static boolean visible(Level level, Vec3 from, Vec3 to, double step, double maxLength, Predicate<BlockState> transparent) {
        if (!Double.isFinite(step) || step < 0.01 || !Double.isFinite(maxLength) || maxLength <= 0 || maxLength > 128)
            throw new IllegalArgumentException("Invalid ray bounds");
        var delta = to.subtract(from); double length = delta.length();
        if (!Double.isFinite(length) || length > maxLength) return false;
        if (length == 0) return true;
        int samples = (int) Math.ceil(length / step); var cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i <= samples; i++) {
            double fraction = i / (double) samples;
            cursor.set(from.x + delta.x * fraction, from.y + delta.y * fraction, from.z + delta.z * fraction);
            if (level.hasChunkAt(cursor) && !transparent.test(level.getBlockState(cursor))) return false;
        }
        return true;
    }
}
