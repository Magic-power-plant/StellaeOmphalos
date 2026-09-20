package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.core.BlockPos;

public record BlockPlacement(BlockPos relative, BlockRule rule, MismatchSeverity severity) {
    public BlockPlacement {
        relative = relative.immutable();
        java.util.Objects.requireNonNull(rule);
        java.util.Objects.requireNonNull(severity);
    }

    public BlockPlacement transform(PlacementTransform t) {
        return new BlockPlacement(t.apply(relative), rule.transform(t), severity);
    }
}
