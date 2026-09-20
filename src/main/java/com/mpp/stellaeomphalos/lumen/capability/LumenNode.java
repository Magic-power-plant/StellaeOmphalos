package com.mpp.stellaeomphalos.lumen.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Runtime identity of one lumen network member. Implementations are position-fixed.
 * This interface never holds a Level; callers pass the world in to avoid binding saves to world instances.
 */
public interface LumenNode {
    BlockPos pos();
    LumenIO io();
    /** Shard index level of this node: floorDiv(y - minBuildHeight, 16); out-of-range heights stay valid. */
    int sectionY();
    /** Called on topology-relevant neighbor changes; true means topology really changed and needs recompute. */
    boolean onNeighborChanged(Level level, BlockPos changed);
    /** Dirty-marker protocol: whether this node's links must be rebuilt. */
    boolean needsRebuild();
    void markClean();
}
