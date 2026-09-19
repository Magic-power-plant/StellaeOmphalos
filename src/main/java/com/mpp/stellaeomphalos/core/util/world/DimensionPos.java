package com.mpp.stellaeomphalos.core.util.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record DimensionPos(ResourceKey<Level> dimension, BlockPos position) {
    public DimensionPos { java.util.Objects.requireNonNull(dimension); position = position.immutable(); }
}
