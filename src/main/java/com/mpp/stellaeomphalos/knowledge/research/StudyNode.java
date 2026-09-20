package com.mpp.stellaeomphalos.knowledge.research;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record StudyNode(
        ResourceLocation id,
        StudyBranch branch,
        int x,
        int y,
        ResourceLocation icon,
        List<ResourceLocation> prerequisites,
        List<ResourceLocation> pages,
        CodexGate gate,
        boolean independent) {
    public StudyNode {
        prerequisites = List.copyOf(prerequisites);
        pages = List.copyOf(pages);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StudyNode node && id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public GateVerdict visibility(GateContext context) {
        var tier = CodexGate.tier(branch.requiredTier()).evaluate(context);
        if (!tier.active()) return tier;
        if (!context.record().branches().contains(branch.name()))
            return new GateVerdict(GateLevel.SILHOUETTE, "branch:" + branch, List.of());
        var verdict = gate.evaluate(context);
        if (!verdict.active()) return verdict;
        if (independent && !context.record().researchedNodes().contains(id))
            return new GateVerdict(GateLevel.READABLE, "node:" + id, List.of());
        return verdict;
    }
}
