package com.mpp.stellaeomphalos.core.util.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;

/** Iterative, bounded discovery only. Breaking is a separate authorized operation. */
public final class TreeHarvester {
    private final ArrayDeque<BlockPos> stack = new ArrayDeque<>();
    private final Set<BlockPos> visited = new HashSet<>();
    private final List<BlockPos> found = new ArrayList<>();
    private final BlockPos origin;
    public TreeHarvester(BlockPos origin) { this.origin = origin.immutable(); stack.push(this.origin); visited.add(this.origin); }
    public boolean advance(Level level, int budget, int limit) {
        if (budget < 1 || limit < 1) throw new IllegalArgumentException("Invalid tree budget");
        for (int i = 0; i < budget && !stack.isEmpty() && found.size() < limit; i++) {
            var pos = stack.pop();
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) continue;
            found.add(pos);
            for (var direction : Direction.values()) {
                var next = pos.relative(direction);
                if (Math.abs(next.getX() - origin.getX()) <= 32 && Math.abs(next.getZ() - origin.getZ()) <= 32
                        && next.getY() >= origin.getY() && next.getY() <= origin.getY() + 64 && visited.add(next)) stack.push(next);
            }
        }
        if (found.size() >= limit) stack.clear();
        return stack.isEmpty();
    }
    public List<BlockPos> result() { return List.copyOf(found); }
}
