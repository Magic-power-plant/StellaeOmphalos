package com.mpp.stellaeomphalos.core.util.world;

import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.core.BlockPos;

public final class ConeBlockWalker implements Iterable<BlockPos> {
    private final BlockPos origin;
    private final int depth;
    private final int radius;
    public ConeBlockWalker(BlockPos origin, int depth, int radius) {
        if (depth <= 0 || radius < 0 || radius > 128) throw new IllegalArgumentException("Invalid cone bounds");
        this.origin = origin.immutable(); this.depth = depth; this.radius = radius;
    }
    @Override public Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            int layer; Iterator<BlockPos> ring = layer();
            private Iterator<BlockPos> layer() {
                int width = (int) ((long) radius * (depth - layer) / depth);
                return new SpiralBlockWalker(origin.below(layer), width).iterator();
            }
            public boolean hasNext() { return layer < depth && ring.hasNext(); }
            public BlockPos next() {
                if (!hasNext()) throw new NoSuchElementException();
                var pos = ring.next();
                if (!ring.hasNext() && ++layer < depth) ring = layer();
                return pos;
            }
        };
    }
}
