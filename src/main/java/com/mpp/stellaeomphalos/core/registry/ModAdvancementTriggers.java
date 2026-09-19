package com.mpp.stellaeomphalos.core.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.ResourceLocation;

/** CriteriaTriggers is not a Forge registry in 1.20.1. */
public final class ModAdvancementTriggers {
    private static final Map<ResourceLocation, Supplier<? extends CriterionTrigger<?>>> ENTRIES = new LinkedHashMap<>();
    private static boolean registered;
    private ModAdvancementTriggers() {}
    public static void declare(ResourceLocation id, Supplier<? extends CriterionTrigger<?>> factory) {
        if (registered || ENTRIES.putIfAbsent(id, factory) != null) throw new IllegalStateException("Duplicate or late trigger " + id);
    }
    public static void register() {
        if (registered) throw new IllegalStateException("Triggers already registered");
        ENTRIES.forEach((id, factory) -> {
            var trigger = factory.get();
            if (!trigger.getId().equals(id)) throw new IllegalStateException("Trigger ID mismatch " + id);
            CriteriaTriggers.register(trigger);
        });
        registered = true;
    }
}
