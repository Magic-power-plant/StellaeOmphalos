package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipeTypes {
    public static final RegistryFamily<RecipeType<?>> TYPES = new RegistryFamily<>(Registries.RECIPE_TYPE);
    public static final RegistryFamily<RecipeSerializer<?>> SERIALIZERS = new RegistryFamily<>(Registries.RECIPE_SERIALIZER);
    private ModRecipeTypes() {}
}
