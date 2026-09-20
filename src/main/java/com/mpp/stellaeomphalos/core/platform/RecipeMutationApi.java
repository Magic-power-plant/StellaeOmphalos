package com.mpp.stellaeomphalos.core.platform;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface RecipeMutationApi {
    void add(ResourceLocation id, JsonObject recipe);

    void remove(ResourceLocation id);

    void replace(ResourceLocation id, JsonObject recipe);

    void scale(ResourceLocation id, String field, double factor);

    void disable(ResourceLocation family);

    void resetToBaseline();

    void resetToBaseline(ResourceLocation family);

    List<ResourceLocation> findRecipes(ResourceLocation family, ItemStack output);
}
