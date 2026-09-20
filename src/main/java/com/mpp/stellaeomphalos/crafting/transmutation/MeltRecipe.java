package com.mpp.stellaeomphalos.crafting.transmutation;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.crafting.altar.recipe.AbstractMachineRecipe;
import com.mpp.stellaeomphalos.data.codec.*;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class MeltRecipe extends AbstractMachineRecipe {
    private final BlockState input;
    private final boolean exact;
    private final WorldResult output;

    public MeltRecipe(ResourceLocation id, JsonObject json) {
        super(id, "melting", json);
        var state = json.getAsJsonObject("input");
        input = RecipeJson.blockState(state);
        exact = state.has("block_state");
        output = new WorldResult(RecipeJson.object(json, "result"));
        if (input.isAir()) throw new IllegalArgumentException("Air cannot melt");
    }

    public boolean matches(BlockState state) {
        return exact ? input.equals(state) : input.is(state.getBlock());
    }

    public BlockState input() {
        return input;
    }

    public WorldResult output() {
        return output;
    }

    @Override
    public int displayDuration() {
        return RecipeJson.integer(definition(), "duration", 100, 1, 1000000);
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

    /**
     * Melting is an effect, not an item-producing machine; the browser supplies its category icon.
     */
    @Override
    public List<ResourceLocation> catalysts() {
        return List.of();
    }
}
