package com.mpp.stellaeomphalos.crafting.grinding;

import com.google.gson.*;
import com.mpp.stellaeomphalos.crafting.altar.recipe.AbstractMachineRecipe;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.*;

public final class GrindwheelRecipe extends AbstractMachineRecipe {
    private final MaterialSpec input;
    private final ResourceLocation alteration;
    private final int chance;
    private final double bonus;

    public GrindwheelRecipe(ResourceLocation id, JsonObject json) {
        super(id, "quern", json);
        input = json.has("input") ? MaterialSpec.parse(json.get("input")) : null;
        alteration =
                json.has("alteration") ? RecipeJson.id(json.get("alteration").getAsString()) : null;
        if (alteration != null) GrindAlterationRegistry.get(alteration);
        if (input == null && alteration == null)
            throw new JsonParseException("Grindwheel needs an input");
        if (input != null && input.fluidRequirement().isPresent())
            throw new JsonParseException("Grindwheel rejects fluids");
        chance =
                RecipeJson.integer(
                        json,
                        "chance",
                        alteration != null && alteration.getPath().equals("hone") ? 40 : 1,
                        1,
                        1000000);
        bonus = RecipeJson.decimal(json, "bonus_chance", 0, 0, 1);
        if (alteration == null && result.empty())
            throw new JsonParseException("Missing quern result");
    }

    @Override
    public boolean matches(Container inventory, Level level) {
        var stack = inventory.getItem(0);
        return !stack.isEmpty()
                && (input == null || input.test(stack))
                && (alteration == null || GrindAlterationRegistry.get(alteration).matches(stack));
    }

    public GrindOutcome grind(ItemStack stack, RandomSource random) {
        if (random.nextInt(chance) != 0)
            return new GrindOutcome(GrindOutcome.Kind.FAIL_SILENT, stack.copy());
        if (alteration != null) return GrindAlterationRegistry.get(alteration).apply(stack, random);
        var output = result.assemble(i -> i == 0 ? stack : ItemStack.EMPTY);
        if (random.nextDouble() < bonus)
            output.setCount(Math.min(output.getMaxStackSize(), output.getCount() * 2));
        return new GrindOutcome(GrindOutcome.Kind.ITEM_CHANGE, output);
    }

    public boolean alteration() {
        return alteration != null;
    }

    public MaterialSpec input() {
        if (input == null) throw new IllegalStateException("Pseudo recipe");
        return input;
    }

    @Override
    public boolean hidden() {
        return alteration != null || super.hidden();
    }

    @Override
    public Map<Integer, MaterialSpec> displaySlots() {
        return input == null ? Map.of() : Map.of(0, input);
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of("chance", chance, "bonus_chance", bonus);
    }
}
