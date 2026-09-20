package com.mpp.stellaeomphalos.structure.match;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Retains the first old state and final new state within a tick. */
public final class BlockChangeBatch {
    public record Change(BlockPos position, BlockState before, BlockState after) {}

    private final Map<BlockPos, Change> changes = new LinkedHashMap<>();
    private final Set<ChunkPos> dirty = new HashSet<>();

    public void add(BlockPos p, BlockState before, BlockState after) {
        p = p.immutable();
        var old = changes.get(p);
        changes.put(p, new Change(p, old == null ? before : old.before(), after));
        if (changes.size() > 64) {
            changes.keySet().forEach(v -> dirty.add(new ChunkPos(v)));
            changes.clear();
        }
    }

    public Collection<Change> changes() {
        return List.copyOf(changes.values());
    }

    public Set<ChunkPos> dirtyChunks() {
        return Set.copyOf(dirty);
    }

    public boolean isEmpty() {
        return changes.isEmpty() && dirty.isEmpty();
    }
}
