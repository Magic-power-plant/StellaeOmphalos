package com.mpp.stellaeomphalos.lumen.transport;

import net.minecraft.core.BlockPos;

/**
 * Sharded routing background work: the proximity queue recomputes nearest-source penalties
 * (5x5 chunk neighborhood, at most `performance.lumenProximityOpsPerTick` positions per tick),
 * and the resolve queue re-warms BFS caches that exceeded their edge budget. No bare threads.
 */
public final class LumenRoutingService {
    private final LumenNetwork network;
    private final LumenWorkQueue proximityDirty = new LumenWorkQueue();
    private final LumenWorkQueue resolveQueue = new LumenWorkQueue();

    LumenRoutingService(LumenNetwork network) { this.network = network; }

    /** Source added/removed: recompute the proximity factor of every source in the 5x5 chunk neighborhood. */
    public void markProximityDirtyAround(BlockPos changed) {
        proximityDirty.offer(changed.asLong());
        int chunkX = changed.getX() >> 4;
        int chunkZ = changed.getZ() >> 4;
        for (var pos : network.sourcePositions())
            if (Math.abs((pos.getX() >> 4) - chunkX) <= 2 && Math.abs((pos.getZ() >> 4) - chunkZ) <= 2)
                proximityDirty.offer(pos.asLong());
    }

    public void enqueueResolve(BlockPos source) { resolveQueue.offer(source.asLong()); }

    /** Processes both queues within their per-tick budgets; leftovers stay queued for the next tick. */
    public void tick(int proximityBudget, long routingBudget) {
        proximityDirty.process(proximityBudget, key -> recomputeProximity(BlockPos.of(key)));
        int resolveBudget = (int) Math.min(routingBudget, Integer.MAX_VALUE);
        resolveQueue.process(resolveBudget, key -> network.warmResolve(BlockPos.of(key), routingBudget));
    }

    private void recomputeProximity(BlockPos pos) {
        if (!network.sourcePositions().contains(pos)) return;
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        double nearest = Double.MAX_VALUE;
        for (var other : network.sourcePositions()) {
            if (other.equals(pos)) continue;
            if (Math.abs((other.getX() >> 4) - chunkX) > 2 || Math.abs((other.getZ() >> 4) - chunkZ) > 2) continue;
            double distance = Math.sqrt(other.distSqr(pos));
            if (distance < nearest) nearest = distance;
        }
        double factor = nearest == Double.MAX_VALUE ? 1 : LumenMath.proximityPenalty(nearest);
        network.updateSourceProximity(pos, factor);
    }

    public int pendingProximity() { return proximityDirty.size(); }
}
