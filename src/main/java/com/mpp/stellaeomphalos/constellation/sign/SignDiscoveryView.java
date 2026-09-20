package com.mpp.stellaeomphalos.constellation.sign;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Read-only view over a player's sign discovery progress; implemented by the player module. */
public interface SignDiscoveryView {
    boolean knowsSign(ResourceLocation signId);
    Set<ResourceLocation> knownSigns();
}
