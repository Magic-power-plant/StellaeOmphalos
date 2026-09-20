package com.mpp.stellaeomphalos.knowledge.research;

import com.mpp.stellaeomphalos.core.platform.*;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Frozen inputs guarantee same-tick idempotence even if authority changes later in the tick. */
public record GateContext(
        KnowledgeSnapshot record,
        ResourceLocation dimension,
        Set<String> stages,
        Set<String> tags,
        boolean stagesInstalled,
        long tick) {
    public GateContext {
        stages = Set.copyOf(stages);
        tags = Set.copyOf(tags);
    }

    public static GateContext of(StarRecordView record) {
        return new GateContext(
                KnowledgeSnapshot.copy(record),
                new ResourceLocation("minecraft:overworld"),
                Set.of(),
                Set.of(),
                false,
                0);
    }
}
