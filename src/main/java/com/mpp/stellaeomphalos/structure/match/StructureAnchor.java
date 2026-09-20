package com.mpp.stellaeomphalos.structure.match;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public interface StructureAnchor {
    ResourceLocation blueprintId();

    default BlockPos originOffset() {
        return BlockPos.ZERO;
    }
}
