package com.mpp.stellaeomphalos.content.world;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.*;

public final class InWaterPlacement extends PlacementFilter {
    public static final Codec<InWaterPlacement> CODEC = Codec.unit(InWaterPlacement::new);

    protected boolean shouldPlace(PlacementContext c, RandomSource r, BlockPos p) {
        return c.getLevel().getFluidState(p).is(FluidTags.WATER)
                || c.getLevel().getFluidState(p.above()).is(FluidTags.WATER)
                || c.getLevel().getBlockState(p).is(Blocks.ICE)
                || c.getLevel().getBlockState(p.above()).is(Blocks.ICE);
    }

    public PlacementModifierType<?> type() {
        return WorldGeneration.IN_WATER.get();
    }
}
