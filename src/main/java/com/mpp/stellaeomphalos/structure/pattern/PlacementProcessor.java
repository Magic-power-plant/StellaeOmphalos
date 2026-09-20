package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

@FunctionalInterface
public interface PlacementProcessor {
    void process(
            ServerLevel level,
            BlockPos origin,
            PlacementTransform transform,
            Set<BlockPos> placed,
            PlacementContext context);
}
