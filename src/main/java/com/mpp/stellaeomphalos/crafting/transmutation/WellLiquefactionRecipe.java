package com.mpp.stellaeomphalos.crafting.transmutation;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.crafting.altar.recipe.AbstractMachineRecipe;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

public final class WellLiquefactionRecipe extends AbstractMachineRecipe {
    private final MaterialSpec input;
    private final FluidIngredient fluid;
    private final double production, shatter;
    private final int color;

    public WellLiquefactionRecipe(ResourceLocation id, JsonObject json) {
        super(id, "well_liquefaction", json);
        input = MaterialSpec.parse(json.get("catalyst"));
        fluid = FluidIngredient.parse(json.getAsJsonObject("result_fluid"));
        production = RecipeJson.decimal(json, "production_multiplier", 1, 0, Double.MAX_VALUE);
        shatter = RecipeJson.decimal(json, "shatter_multiplier", 0, 0, Double.MAX_VALUE);
        color = json.has("catalyst_color") ? json.get("catalyst_color").getAsInt() : -1;
    }

    public int gain(long stored) {
        return (int) Math.min(Integer.MAX_VALUE, Math.sqrt(Math.max(0, stored)) * production);
    }

    public boolean shatters(RandomSource random) {
        int bound = (int) Math.min(Integer.MAX_VALUE, 1 + 1000 * shatter);
        return random.nextInt(2000) == 0 || random.nextInt(Math.max(1, bound)) == 0;
    }

    public FluidStack produce(long stored) {
        return new FluidStack(fluid.fluids().get(0), gain(stored));
    }

    public int color() {
        return color;
    }

    public MaterialSpec input() {
        return input;
    }

    @Override
    public Map<Integer, MaterialSpec> displaySlots() {
        return Map.of(0, input);
    }

    @Override
    public List<FluidStack> displayFluidOutputs() {
        return List.of(new FluidStack(fluid.fluids().get(0), fluid.amount()));
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of("production_multiplier", production, "shatter_multiplier", shatter);
    }

    @Override
    public List<ResourceLocation> catalysts() {
        return List.of(new ResourceLocation("stellaeomphalos", "lumen_well"));
    }
}
