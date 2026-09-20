package com.mpp.stellaeomphalos.crafting.altar.effect;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

/** Registered factories never share effect instances between tasks. */
public final class AsterismEffectRegistry {
    private static final Map<ResourceLocation, Supplier<AsterismEffectProvider>> FACTORIES =
            new HashMap<>();
    private static final AsterismEffectProvider SILENT =
            new AsterismEffectProvider() {
                public void tick(ClientCraftView view) {}

                public void finish(ClientCraftView view) {}
            };

    private AsterismEffectRegistry() {}

    public static void register(ResourceLocation id, Supplier<AsterismEffectProvider> factory) {
        if (FACTORIES.putIfAbsent(id, factory) != null)
            throw new IllegalArgumentException("Duplicate effect");
    }

    public static AsterismEffectProvider create(ResourceLocation id) {
        return FACTORIES.getOrDefault(id, () -> SILENT).get();
    }

    /** A renderer owns one cache for one task and closes it when the task is replaced. */
    public static final class TaskEffects implements AutoCloseable {
        private record Key(ResourceLocation effect, int index) {}

        private final Map<Key, AsterismEffectProvider> effects = new HashMap<>();

        public AsterismEffectProvider get(ResourceLocation effect, int index) {
            return effects.computeIfAbsent(new Key(effect, index), key -> create(key.effect()));
        }

        @Override
        public void close() {
            effects.values().forEach(AsterismEffectProvider::close);
            effects.clear();
        }
    }
}
