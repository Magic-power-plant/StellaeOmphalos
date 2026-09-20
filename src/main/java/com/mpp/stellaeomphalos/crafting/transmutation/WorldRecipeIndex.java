package com.mpp.stellaeomphalos.crafting.transmutation;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.core.platform.WorldCraftingBridge;
import com.mpp.stellaeomphalos.crafting.altar.recipe.CraftingBootstrap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.*;

/** Dedicated world-state and ordered fluid-pair indices, including an indexed furnace fallback. */
public final class WorldRecipeIndex {
    private record FluidPair(Fluid first, Fluid second) {}

    private static final Map<net.minecraft.server.MinecraftServer, WorldRecipeIndex> SESSIONS =
            new WeakHashMap<>();
    private int epoch = -1;
    private final Map<Block, List<LightTransmutationRecipe>> transmutation = new HashMap<>();
    private final Map<Block, List<MeltRecipe>> melting = new HashMap<>();
    private final Map<Block, WorldCraftingBridge.MeltOperation> furnace = new HashMap<>();
    private final Map<FluidPair, List<FluidInteractionRecipe>> interactions = new HashMap<>();

    private WorldRecipeIndex() {}

    public static WorldRecipeIndex of(ServerLevel level) {
        var index = SESSIONS.computeIfAbsent(level.getServer(), s -> new WorldRecipeIndex());
        int epoch = CraftingBootstrap.hub(level.getServer()).epoch();
        if (index.epoch != epoch) index.rebuild(level, epoch);
        return index;
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private void rebuild(ServerLevel level, int epoch) {
        this.epoch = epoch;
        transmutation.clear();
        melting.clear();
        furnace.clear();
        interactions.clear();
        var hub = CraftingBootstrap.hub(level.getServer());
        for (var r : hub.all(CraftingBootstrap.id("light_transmutation"))) {
            var recipe = (LightTransmutationRecipe) r;
            transmutation
                    .computeIfAbsent(recipe.input().getBlock(), k -> new ArrayList<>())
                    .add(recipe);
        }
        for (var r : hub.all(CraftingBootstrap.id("melting"))) {
            var recipe = (MeltRecipe) r;
            melting.computeIfAbsent(recipe.input().getBlock(), k -> new ArrayList<>()).add(recipe);
        }
        for (var r : hub.all(CraftingBootstrap.id("fluid_interaction"))) {
            var recipe = (FluidInteractionRecipe) r;
            for (var a : recipe.first().fluids())
                for (var b : recipe.second().fluids()) {
                    interactions
                            .computeIfAbsent(new FluidPair(a, b), k -> new ArrayList<>())
                            .add(recipe);
                    if (a != b)
                        interactions
                                .computeIfAbsent(new FluidPair(b, a), k -> new ArrayList<>())
                                .add(recipe);
                }
        }
        var recipes =
                level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream()
                        .sorted(Comparator.comparing(r -> r.getId().toString()))
                        .toList();
        for (var recipe : recipes) {
            var result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            var json = new JsonObject();
            if (result.getItem() instanceof BlockItem block)
                json.addProperty(
                        "block",
                        net.minecraftforge.registries.ForgeRegistries.BLOCKS
                                .getKey(block.getBlock())
                                .toString());
            else {
                json.addProperty(
                        "item",
                        net.minecraftforge.registries.ForgeRegistries.ITEMS
                                .getKey(result.getItem())
                                .toString());
                json.addProperty("count", result.getCount());
            }
            var output = new WorldResult(json);
            var operation = new MeltOperation(Math.max(1, recipe.getCookingTime() / 2), output);
            for (var ingredient : recipe.getIngredients())
                for (var input : ingredient.getItems())
                    if (input.getItem() instanceof BlockItem block)
                        furnace.putIfAbsent(block.getBlock(), operation);
        }
    }

    public List<LightTransmutationRecipe> transmutation(BlockState state) {
        return List.copyOf(transmutation.getOrDefault(state.getBlock(), List.of()));
    }

    public List<FluidInteractionRecipe> interactions(Fluid first, Fluid second) {
        return List.copyOf(interactions.getOrDefault(new FluidPair(first, second), List.of()));
    }

    public Optional<WorldCraftingBridge.MeltOperation> melting(BlockState state) {
        for (var recipe : melting.getOrDefault(state.getBlock(), List.of()))
            if (recipe.matches(state))
                return Optional.of(new MeltOperation(recipe.displayDuration(), recipe.output()));
        return Optional.ofNullable(furnace.get(state.getBlock()));
    }

    private record MeltOperation(int duration, WorldResult output)
            implements WorldCraftingBridge.MeltOperation {
        public boolean apply(ServerLevel level, BlockPos pos, BlockState expected) {
            return output.apply(level, pos, expected);
        }
    }
}
