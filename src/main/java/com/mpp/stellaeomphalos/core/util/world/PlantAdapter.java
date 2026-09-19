package com.mpp.stellaeomphalos.core.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class PlantAdapter {
    private PlantAdapter() {}
    public static boolean mature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        return state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) == 3;
    }
    public static boolean grow(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(level, pos, state, false)
                && plant.isBonemealSuccess(level, level.random, pos, state)) {
            plant.performBonemeal(level, level.random, pos, state); return true;
        }
        return false;
    }
}
