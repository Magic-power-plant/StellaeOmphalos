package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.lumen.capability.LumenPrism;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Splitting end: distributes input across outputs by weight; empty weights mean lossless pass-through. */
public abstract class LumenPrismBlockEntity extends LumenInventoryBlockEntity implements LumenPrism {
    protected LumenPrismBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, EnumSet<Direction> faces) {
        super(type, pos, state, slots, faces);
    }
    @Override public final LumenIO io() { return LumenIO.PRISM; }
    @Override public List<Double> splitWeights(Direction from) { return List.of(); }
}
