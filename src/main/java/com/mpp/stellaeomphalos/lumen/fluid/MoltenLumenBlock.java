package com.mpp.stellaeomphalos.lumen.fluid;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class MoltenLumenBlock extends LiquidBlock {
    public MoltenLumenBlock(Supplier<? extends FlowingFluid> fluid) {
        super(fluid, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).replaceable().noCollission()
                .randomTicks().strength(100.0F).lightLevel(state -> 15).pushReaction(PushReaction.DESTROY)
                .noLootTable().liquid());
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        LumenFluidInteractions.onEntityInside(level, pos, entity);
    }
}
