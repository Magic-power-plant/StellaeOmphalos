package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Transit end: no source, no sink; participates in per-hop loss and throughput only. */
public abstract class LumenRelayBlockEntity extends LumenTickBlockEntity {
    protected LumenRelayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public final LumenIO io() { return LumenIO.RELAY; }
}
