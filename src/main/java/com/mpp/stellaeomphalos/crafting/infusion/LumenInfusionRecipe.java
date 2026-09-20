package com.mpp.stellaeomphalos.crafting.infusion;

import com.google.gson.*;
import com.mpp.stellaeomphalos.crafting.altar.recipe.AbstractMachineRecipe;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class LumenInfusionRecipe extends AbstractMachineRecipe {
    private final MaterialSpec input;
    private final FluidIngredient solvent;
    private final double chance, acceleration;
    private final boolean multiple, direct;
    private final String consumeMode;
    private final ResourceLocation advancement;

    public LumenInfusionRecipe(ResourceLocation id, JsonObject json) {
        super(id, "lumen_infusion", json);
        input = MaterialSpec.parse(json.get("input"));
        var fluid = RecipeJson.object(json, "solvent").deepCopy();
        if (!fluid.has("fluid")) fluid.addProperty("fluid", "stellaeomphalos:molten_lumen");
        if (!fluid.has("amount")) fluid.addProperty("amount", 400);
        solvent = FluidIngredient.parse(fluid);
        chance = RecipeJson.decimal(fluid, "chance", 0.1, 0, 1);
        acceleration = RecipeJson.decimal(fluid, "acceleration", 0.3, 0.01, 1);
        multiple = RecipeJson.flag(fluid, "per_chalice", false);
        direct = RecipeJson.flag(fluid, "allow_direct_block_drain", true);
        consumeMode = RecipeJson.text(json, "consume_mode", "decrement");
        if (!Set.of("decrement", "replace_with_container", "keep").contains(consumeMode))
            throw new JsonParseException("Invalid consume mode");
        advancement =
                json.has("required_advancement")
                        ? RecipeJson.id(json.get("required_advancement").getAsString())
                        : null;
    }

    public MaterialSpec input() {
        return input;
    }

    public FluidIngredient solvent() {
        return solvent;
    }

    public double chance() {
        return chance;
    }

    public double acceleration() {
        return acceleration;
    }

    public int chaliceRequiredAmount() {
        return Math.max(1, (int) Math.floor(chance * solvent.amount() * (multiple ? 12 : 1)));
    }

    public boolean multiple() {
        return multiple;
    }

    public boolean allowDirect() {
        return direct;
    }

    public boolean consumes() {
        return !consumeMode.equals("keep");
    }

    public Optional<ResourceLocation> advancement() {
        return Optional.ofNullable(advancement);
    }

    @Override
    public Map<Integer, MaterialSpec> displaySlots() {
        return Map.of(0, input);
    }

    @Override
    public List<ResourceLocation> catalysts() {
        return List.of(new ResourceLocation("stellaeomphalos", "lumen_infuser"));
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of("chance", chance, "acceleration", acceleration, "amount", solvent.amount());
    }
}
