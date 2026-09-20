package com.mpp.stellaeomphalos.knowledge.advancement;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.registry.ModAdvancementTriggers;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class ModMilestones {
    private static final Map<String, MilestoneTrigger> triggers = new LinkedHashMap<>();

    private ModMilestones() {}

    public static void initialize() {
        if (!triggers.isEmpty()) return;
        for (var name : List.of("sign", "rite", "altar", "shard", "boon", "resonance")) {
            var id = new ResourceLocation(Omphalos.MODID, name + "_milestone");
            var trigger = new MilestoneTrigger(id) {};
            triggers.put(name, trigger);
            ModAdvancementTriggers.declare(id, () -> trigger);
        }
    }

    public static Optional<MilestoneTrigger> find(String name) {
        return Optional.ofNullable(triggers.get(name));
    }
}
