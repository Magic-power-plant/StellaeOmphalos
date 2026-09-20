package com.mpp.stellaeomphalos.constellation.boon;

import net.minecraft.resources.ResourceLocation;

/** One node's client layout entry: identity, grid position and node kind (ordinal of BoonNodeType). */
public record BoonNodeLayout(ResourceLocation id, int gridX, int gridZ, byte kind) {
    public BoonNodeLayout {
        if (id == null) throw new IllegalArgumentException("null layout id");
    }

    public BoonNodeType type() {
        return BoonNodeType.values()[kind];
    }
}
