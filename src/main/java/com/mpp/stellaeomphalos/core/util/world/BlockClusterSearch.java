package com.mpp.stellaeomphalos.core.util.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Resumable BFS with an explicit work budget and bounded search volume. Predicate owns loaded-chunk checks. */
public final class BlockClusterSearch {
    private final Queue<BlockPos> frontier = new ArrayDeque<>();
    private final Set<BlockPos> visited = new HashSet<>();
    private final List<BlockPos> matched = new ArrayList<>();
    private final BlockPos origin;
    private final int radius;
    private final int maxMatches;
    private final Predicate<BlockPos> predicate;
    public BlockClusterSearch(BlockPos origin, int radius, int maxMatches, Predicate<BlockPos> predicate) {
        if (radius < 0 || maxMatches < 1) throw new IllegalArgumentException("Invalid search bounds");
        this.origin = origin.immutable(); this.radius = radius; this.maxMatches = maxMatches; this.predicate = predicate;
        frontier.add(this.origin); visited.add(this.origin);
    }
    public boolean advance(int budget) {
        if (budget < 1) throw new IllegalArgumentException("Invalid scan budget");
        for (int scanned = 0; scanned < budget && !frontier.isEmpty() && matched.size() < maxMatches; scanned++) {
            var pos = frontier.remove();
            if (!predicate.test(pos)) continue;
            matched.add(pos);
            for (var direction : Direction.values()) {
                var next = pos.relative(direction);
                if (Math.abs((long) next.getX() - origin.getX()) <= radius && Math.abs((long) next.getY() - origin.getY()) <= radius
                        && Math.abs((long) next.getZ() - origin.getZ()) <= radius && visited.add(next)) frontier.add(next);
            }
        }
        if (matched.size() == maxMatches) frontier.clear();
        return frontier.isEmpty();
    }
    public List<BlockPos> result() { return List.copyOf(matched); }
}
