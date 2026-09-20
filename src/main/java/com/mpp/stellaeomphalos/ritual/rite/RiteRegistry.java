package com.mpp.stellaeomphalos.ritual.rite;

import com.mpp.stellaeomphalos.ritual.amplifier.AmplifierTier;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class RiteRegistry {
    private static volatile Map<ResourceLocation, RiteRecipe> recipes = Map.of();
    private static volatile Map<ResourceLocation, AmplifierTier> amplifiers = Map.of();

    private RiteRegistry() {}

    public static Map<ResourceLocation, RiteRecipe> recipes() {
        return recipes;
    }

    public static Map<ResourceLocation, AmplifierTier> amplifiers() {
        return amplifiers;
    }

    public static void recipes(Map<ResourceLocation, RiteRecipe> value) {
        recipes = Map.copyOf(value);
    }

    public static void amplifiers(Map<ResourceLocation, AmplifierTier> value) {
        amplifiers = Map.copyOf(value);
    }
}
