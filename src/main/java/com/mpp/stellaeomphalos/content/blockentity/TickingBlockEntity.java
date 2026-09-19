package com.mpp.stellaeomphalos.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Age is session-local and firstTick runs once per load, always on the logical server. */
public abstract class TickingBlockEntity extends SyncedBlockEntity {
    private long age;
    private boolean initialized;
    protected TickingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    protected void firstTick() {}
    protected abstract void serverTick();
    protected final long age() { return age; }
    public static void tick(Level level, BlockPos pos, BlockState state, TickingBlockEntity entity) {
        if (level.isClientSide || entity.isRemoved()) return;
        if (!initialized(entity)) entity.firstTick();
        entity.serverTick(); entity.age++;
    }
    private static boolean initialized(TickingBlockEntity entity) {
        boolean previous = entity.initialized; entity.initialized = true; return previous;
    }
    @Override public void onLoad() { super.onLoad(); initialized = false; age = 0; }
}
