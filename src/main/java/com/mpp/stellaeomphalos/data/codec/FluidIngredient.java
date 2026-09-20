package com.mpp.stellaeomphalos.data.codec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Exact millibucket requirement, independent of item ingredient previews. */
public record FluidIngredient(ResourceLocation id, boolean tag, int amount) {
    public FluidIngredient {
        if (amount < 1 || amount > 1000000)
            throw new IllegalArgumentException("Invalid fluid amount");
    }

    public static FluidIngredient parse(JsonObject json) {
        boolean tag = json.has("fluid_tag");
        var id = RecipeJson.id(RecipeJson.text(json, tag ? "fluid_tag" : "fluid", ""));
        if (!tag && !ForgeRegistries.FLUIDS.containsKey(id))
            throw new JsonParseException("Unknown fluid " + id);
        return new FluidIngredient(id, tag, RecipeJson.integer(json, "amount", 1000, 1, 1000000));
    }

    public boolean test(FluidStack stack) {
        return !stack.isEmpty() && stack.getAmount() >= amount && accepts(stack.getFluid());
    }

    public boolean accepts(Fluid fluid) {
        return tag
                ? fluid.is(TagKey.create(Registries.FLUID, id))
                : id.equals(ForgeRegistries.FLUIDS.getKey(fluid));
    }

    public List<Fluid> fluids() {
        if (!tag) return List.of(ForgeRegistries.FLUIDS.getValue(id));
        var values = ForgeRegistries.FLUIDS.tags().getTag(TagKey.create(Registries.FLUID, id));
        return values.stream().toList();
    }
}
