package com.mpp.stellaeomphalos.ritual.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

/** Bounded reusable offset geometry; iteration filters unloaded chunks without requesting them. */
public final class AffectedRegion implements Iterable<BlockPos> {
    public enum InvalidationReason {
        CHUNK_UNLOAD,
        CHUNK_LOAD,
        RADIUS_CHANGED,
        STRUCTURE_CHANGED,
        SIGN_CHANGED,
        INTENSITY_CHANGED,
        MANUAL
    }

    private final ServerLevel level;
    private final BlockPos origin;
    private final List<BlockPos> offsets;
    private boolean invalid;

    public AffectedRegion(ServerLevel level, BlockPos origin, double radius) {
        if (!Double.isFinite(radius) || radius < 0 || radius > 32)
            throw new IllegalArgumentException("Region radius");
        this.level = level;
        this.origin = origin.immutable();
        var cells = new ArrayList<BlockPos>();
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x++)
            for (int y = -r; y <= r; y++)
                for (int z = -r; z <= r; z++)
                    if (x * x + y * y + z * z <= radius * radius) cells.add(new BlockPos(x, y, z));
        offsets = List.copyOf(cells);
    }

    public int expectedCount() {
        return offsets.size();
    }

    public int loadedCount() {
        return (int) offsets.stream().map(origin::offset).filter(level::hasChunkAt).count();
    }

    public boolean partiallyUnloaded() {
        return loadedCount() != expectedCount();
    }

    public void invalidate(InvalidationReason reason) {
        invalid = true;
    }

    public boolean invalid() {
        return invalid;
    }

    public Iterator<BlockPos> iterator() {
        return offsets.stream().map(origin::offset).filter(level::hasChunkAt).iterator();
    }
}
