package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.util.RandomSource;

import java.util.UUID;

public record PlacementContext(
        Source source,
        UUID actor,
        boolean allowReplaceFluid,
        boolean suppressIntegrity,
        long seed) {
    public enum Source {
        WORLDGEN,
        COMMAND,
        SCHEMATIC_PASTE,
        RITE_ASSIST
    }

    public RandomSource random() {
        return RandomSource.create(seed);
    }
}
