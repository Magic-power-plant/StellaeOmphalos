package com.mpp.stellaeomphalos.crafting.transmutation;

import com.google.gson.*;
import com.mpp.stellaeomphalos.crafting.altar.recipe.AbstractMachineRecipe;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class LightTransmutationRecipe extends AbstractMachineRecipe {
    private final BlockState input;
    private final boolean exact;
    private final WorldResult output;
    private final long cost;
    private final ResourceLocation sign;

    public LightTransmutationRecipe(ResourceLocation id, JsonObject json) {
        super(id, "light_transmutation", json);
        var block = RecipeJson.object(json, "input");
        input = RecipeJson.blockState(block);
        exact = block.has("block_state");
        if (input.isAir() || input.is(Blocks.CRAFTING_TABLE))
            throw new JsonParseException("Invalid transmutation input");
        output = new WorldResult(RecipeJson.object(json, "result"));
        if (output.empty()) throw new JsonParseException("Empty transmutation result");
        cost = RecipeJson.amount(json, "cost", 1);
        sign =
                json.has("required_sign")
                        ? RecipeJson.id(json.get("required_sign").getAsString())
                        : null;
    }

    public boolean matches(BlockState state, long lumen, Set<ResourceLocation> signs) {
        return (exact ? input.equals(state) : input.is(state.getBlock()))
                && lumen >= cost
                && (sign == null || signs.contains(sign));
    }

    public BlockState input() {
        return input;
    }

    public WorldResult output() {
        return output;
    }

    public long cost() {
        return cost;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return output.display();
    }

    @Override
    public Map<Integer, MaterialSpec> displaySlots() {
        return Map.of(
                0,
                new MaterialSpec.ItemMaterial(
                        net.minecraft.world.item.crafting.Ingredient.of(input.getBlock()),
                        new net.minecraft.nbt.CompoundTag(),
                        new net.minecraft.nbt.CompoundTag()));
    }

    @Override
    public Map<String, Number> displayScalars() {
        return Map.of("cost", cost);
    }

    @Override
    public List<ResourceLocation> catalysts() {
        return List.of(new ResourceLocation("stellaeomphalos", "light_transmuter"));
    }
}
