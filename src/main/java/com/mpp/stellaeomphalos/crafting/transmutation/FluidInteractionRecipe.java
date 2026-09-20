package com.mpp.stellaeomphalos.crafting.transmutation;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.crafting.altar.recipe.AbstractMachineRecipe;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

public final class FluidInteractionRecipe extends AbstractMachineRecipe {
    private final FluidIngredient first, second;
    private final double firstChance, secondChance;
    private final int weight;
    private final WorldResult output;

    public FluidInteractionRecipe(ResourceLocation id, JsonObject json) {
        super(id, "fluid_interaction", json);
        first = FluidIngredient.parse(json.getAsJsonObject("first"));
        second = FluidIngredient.parse(json.getAsJsonObject("second"));
        firstChance = RecipeJson.decimal(json, "consume_chance_first", 1, 0, 1);
        secondChance = RecipeJson.decimal(json, "consume_chance_second", 1, 0, 1);
        weight = RecipeJson.integer(json, "weight", 1, 0, 1000000);
        output = new WorldResult(json.getAsJsonObject("result"));
        if (output.empty()) throw new IllegalArgumentException("Empty interaction result");
    }

    public boolean matches(FluidStack a, FluidStack b) {
        return first.test(a) && second.test(b);
    }

    public FluidIngredient first() {
        return first;
    }

    public FluidIngredient second() {
        return second;
    }

    public double firstChance() {
        return firstChance;
    }

    public double secondChance() {
        return secondChance;
    }

    public int weight() {
        return weight;
    }

    public WorldResult output() {
        return output;
    }

    @Override
    public Map<Integer, MaterialSpec> displaySlots() {
        return Map.of(
                0,
                new MaterialSpec.FluidMaterial(first),
                1,
                new MaterialSpec.FluidMaterial(second));
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return output.display();
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of(
                "weight",
                weight,
                "consume_chance_first",
                firstChance,
                "consume_chance_second",
                secondChance);
    }

    @Override
    public List<ResourceLocation> catalysts() {
        return List.of(new ResourceLocation("stellaeomphalos", "lumen_chalice"));
    }
}
