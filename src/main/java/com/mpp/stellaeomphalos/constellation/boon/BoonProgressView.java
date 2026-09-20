package com.mpp.stellaeomphalos.constellation.boon;

import net.minecraft.resources.ResourceLocation;

/** Read-only view over a player's boon progress; implemented by the player module's progress store. */
public interface BoonProgressView {
    boolean hasNode(ResourceLocation nodeId);

    boolean isSealed(ResourceLocation nodeId);

    int availablePoints();

    boolean knowsSign(ResourceLocation signId);

    int level();
}
