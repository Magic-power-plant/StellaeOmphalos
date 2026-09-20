package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.blockentity.rite.LumenSpringBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;

public final class LumenSpringBlock extends Block implements EntityBlock {
    public LumenSpringBlock() {
        super(Properties.of().strength(3, 10));
    }

    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new LumenSpringBlockEntity(p, s);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level l, BlockState s, BlockEntityType<T> type) {
        return l.isClientSide
                ? null
                : (world, p, state, be) -> {
                    if (be instanceof LumenSpringBlockEntity spring) spring.serverTick();
                };
    }

    public InteractionResult use(
            BlockState s,
            Level level,
            BlockPos p,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        return FluidUtil.interactWithFluidHandler(player, hand, level, p, hit.getDirection())
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.PASS;
    }
}
