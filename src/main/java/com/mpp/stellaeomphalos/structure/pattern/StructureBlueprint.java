package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.*;

public interface StructureBlueprint {
    default boolean noPaste() {
        return false;
    }

    ResourceLocation id();

    Map<BlockPos, BlockPlacement> blocks();

    Set<BlockPos> uniqueSlots();

    boolean mirrorable();

    default BoundingBox bounds() {
        return BoundingBox.encapsulatingPositions(blocks().keySet())
                .orElse(new BoundingBox(BlockPos.ZERO));
    }
}
