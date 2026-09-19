package com.mpp.stellaeomphalos.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Sky visibility is rebuilt after load and sampled every sixteen server ticks. */
public abstract class SkyAwareBlockEntity extends TickingBlockEntity {
    private boolean skyVisible;
    protected SkyAwareBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    protected final boolean skyVisible() { return skyVisible; }
    @Override protected final void serverTick() {
        if (age() % 16 == 0) skyVisible = level != null && level.canSeeSky(worldPosition.above());
        tickWithSky(skyVisible);
    }
    protected abstract void tickWithSky(boolean visible);
}
