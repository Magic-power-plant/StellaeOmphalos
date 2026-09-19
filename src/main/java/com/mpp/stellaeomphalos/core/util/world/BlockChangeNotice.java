package com.mpp.stellaeomphalos.core.util.world;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Event;

/** Actual post-change state notification, dispatched synchronously on the server thread. */
public final class BlockChangeNotice extends Event {
    private final DimensionPos position;
    private final BlockState before;
    private final BlockState after;
    public BlockChangeNotice(DimensionPos position, BlockState before, BlockState after) {
        this.position = position; this.before = before; this.after = after;
    }
    public DimensionPos position() { return position; }
    public BlockState before() { return before; }
    public BlockState after() { return after; }
}
