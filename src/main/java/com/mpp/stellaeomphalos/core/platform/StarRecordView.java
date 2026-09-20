package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Side-neutral knowledge capability. Implementations never expose mutable collections. */
public interface StarRecordView {
    boolean valid();

    StarTier tier();

    Set<ResourceLocation> knownSigns();

    Set<ResourceLocation> seenSigns();

    Set<String> branches();

    Set<ResourceLocation> researchedNodes();

    Set<ResourceLocation> unlockedShards();

    Set<ResourceLocation> usedTargets();

    Map<ResourceLocation, Integer> codexSeen();

    String lastRoute();

    boolean firstJoinRewarded();

    long revision();
}
