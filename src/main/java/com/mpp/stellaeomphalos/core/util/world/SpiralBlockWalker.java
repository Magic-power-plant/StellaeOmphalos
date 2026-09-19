package com.mpp.stellaeomphalos.core.util.world;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.core.BlockPos;

public final class SpiralBlockWalker implements Iterable<BlockPos> {
    private final BlockPos origin;
    private final int radius;
    public SpiralBlockWalker(BlockPos origin, int radius) {
        if (radius < 0 || radius > 10000) throw new IllegalArgumentException("Invalid radius");
        this.origin = origin.immutable(); this.radius = radius;
    }
    @Override public Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            final long count = (2L * radius + 1) * (2L * radius + 1);
            long emitted; int x; int z; int dx = 1; int dz; int leg = 1; int progress; int turns;
            public boolean hasNext() { return emitted < count; }
            public BlockPos next() {
                if (!hasNext()) throw new NoSuchElementException();
                var result = origin.offset(x, 0, z); emitted++;
                x += dx; z += dz; progress++;
                if (progress == leg) {
                    progress = 0; int oldDx = dx; dx = -dz; dz = oldDx;
                    if (++turns % 2 == 0) leg++;
                }
                return result;
            }
        };
    }
}
