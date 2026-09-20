package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** One atomic reload snapshot; watches keep ids and reconcile against the epoch. */
public final class BlueprintRegistry {
    private static volatile Map<ResourceLocation, BuildBlueprint> snapshot = Map.of();
    private static volatile int epoch;

    private BlueprintRegistry() {}

    public static Optional<BuildBlueprint> find(ResourceLocation id) {
        return Optional.ofNullable(snapshot.get(id));
    }

    public static Map<ResourceLocation, BuildBlueprint> all() {
        return snapshot;
    }

    public static int epoch() {
        return epoch;
    }

    public static void publish(Map<ResourceLocation, BuildBlueprint> next) {
        snapshot = Map.copyOf(next);
        epoch++;
    }
}
