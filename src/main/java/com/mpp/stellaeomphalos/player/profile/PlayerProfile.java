package com.mpp.stellaeomphalos.player.profile;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative player progress capability. */
public interface PlayerProfile {
    int schema();
    Set<ResourceLocation> knownSigns();
    Set<ResourceLocation> seenSigns();
    Set<ResourceLocation> researchGroups();
    Set<ResourceLocation> unlockedBoons();
    long boonExperience();
    int freeTokens();
    ResourceLocation attunedSign();
    boolean wasAttuned();
    void discoverSign(ResourceLocation id);
    void markSeen(ResourceLocation id);
    void setAttuned(ResourceLocation id);
    void grantResearch(ResourceLocation id);
    void grantExperience(long amount);
    void setFreeTokens(int amount);
    void resetProgress();
    void load(CompoundTag tag);
    CompoundTag save();
    void copyFrom(PlayerProfile other);
}
