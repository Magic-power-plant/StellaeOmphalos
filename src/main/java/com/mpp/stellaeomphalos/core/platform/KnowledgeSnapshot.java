package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Immutable mirror and gate input; constructing it does not touch a world. */
public record KnowledgeSnapshot(
        boolean valid,
        StarTier tier,
        Set<ResourceLocation> knownSigns,
        Set<ResourceLocation> seenSigns,
        Set<String> branches,
        Set<ResourceLocation> researchedNodes,
        Set<ResourceLocation> unlockedShards,
        Set<ResourceLocation> usedTargets,
        Map<ResourceLocation, Integer> codexSeen,
        String lastRoute,
        boolean firstJoinRewarded,
        long revision)
        implements StarRecordView {
    public KnowledgeSnapshot {
        knownSigns = Set.copyOf(knownSigns);
        seenSigns = Set.copyOf(seenSigns);
        branches = Set.copyOf(branches);
        researchedNodes = Set.copyOf(researchedNodes);
        unlockedShards = Set.copyOf(unlockedShards);
        usedTargets = Set.copyOf(usedTargets);
        codexSeen = Map.copyOf(codexSeen);
    }

    public static KnowledgeSnapshot copy(StarRecordView r) {
        return new KnowledgeSnapshot(
                r.valid(),
                r.tier(),
                r.knownSigns(),
                r.seenSigns(),
                r.branches(),
                r.researchedNodes(),
                r.unlockedShards(),
                r.usedTargets(),
                r.codexSeen(),
                r.lastRoute(),
                r.firstJoinRewarded(),
                r.revision());
    }

    public static KnowledgeSnapshot empty() {
        return from(new CompoundTag(), 0);
    }

    public static KnowledgeSnapshot from(CompoundTag tag, long revision) {
        var branches = new LinkedHashSet<String>();
        branches.add("DISCOVERY");
        tag.getList("Branches", Tag.TAG_STRING).forEach(v -> branches.add(v.getAsString()));
        var pages = new LinkedHashMap<ResourceLocation, Integer>();
        var read = tag.getCompound("CodexSeen");
        read.getAllKeys()
                .forEach(
                        key -> {
                            var id = ResourceLocation.tryParse(key);
                            if (id != null) pages.put(id, read.getInt(key));
                        });
        return new KnowledgeSnapshot(
                true,
                StarTier.parse(tag.getString("Tier")),
                ids(tag, "KnownSigns"),
                ids(tag, "SeenSigns"),
                branches,
                ids(tag, "ResearchedNodes"),
                ids(tag, "UnlockedShards"),
                ids(tag, "UsedTargets"),
                pages,
                tag.getString("CodexLastRoute"),
                tag.getBoolean("FirstJoinRewarded"),
                revision);
    }

    private static Set<ResourceLocation> ids(CompoundTag tag, String key) {
        var values = new LinkedHashSet<ResourceLocation>();
        tag.getList(key, Tag.TAG_STRING)
                .forEach(
                        v -> {
                            var id = ResourceLocation.tryParse(v.getAsString());
                            if (id != null) values.add(id);
                        });
        return values;
    }
}
