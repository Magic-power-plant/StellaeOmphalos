package com.mpp.stellaeomphalos.data.registry;

import com.mpp.stellaeomphalos.core.platform.RecipeDefinition;
import com.mpp.stellaeomphalos.data.codec.MaterialSpec;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidUtil;

import java.util.*;

/** Hash candidate buckets choose the least frequent item handle, with a fluid-capability bucket. */
public final class RecipeIndex {
    private final Map<Item, List<Recipe<?>>> inputs = new HashMap<>(), outputs = new HashMap<>();
    private final Map<Fluid, List<Recipe<?>>> fluids = new HashMap<>();
    private final List<Recipe<?>> wildcard = new ArrayList<>();
    private final List<Recipe<?>> all;

    public RecipeIndex(Collection<Recipe<?>> recipes) {
        all =
                recipes.stream()
                        .sorted(
                                Comparator.<Recipe<?>>comparingInt(
                                                r -> ((RecipeDefinition) r).priority())
                                        .reversed()
                                        .thenComparing(r -> r.getId().toString()))
                        .toList();
        var frequency = new HashMap<Item, Integer>();
        for (var recipe : all)
            for (var spec : ((RecipeDefinition) recipe).displaySlots().values())
                for (var item : spec.displayStacks())
                    frequency.merge(item.getItem(), 1, Integer::sum);
        for (var recipe : all) {
            var data = (RecipeDefinition) recipe;
            MaterialSpec anchor =
                    data.displaySlots().values().stream()
                            .min(
                                    Comparator.comparingInt(
                                            s ->
                                                    s.fluidRequirement().isPresent()
                                                            ? Integer.MAX_VALUE - 1
                                                            : s.displayStacks().stream()
                                                                    .mapToInt(
                                                                            i ->
                                                                                    frequency
                                                                                            .getOrDefault(
                                                                                                    i
                                                                                                            .getItem(),
                                                                                                    0))
                                                                    .sum()))
                            .orElse(null);
            if (anchor == null) wildcard.add(recipe);
            else if (anchor.fluidRequirement().isPresent())
                for (var fluid : anchor.fluidRequirement().get().fluids())
                    fluids.computeIfAbsent(fluid, k -> new ArrayList<>()).add(recipe);
            else
                for (var item : anchor.displayStacks())
                    inputs.computeIfAbsent(item.getItem(), k -> new ArrayList<>()).add(recipe);
            for (var item : data.displayOutputs())
                if (!item.isEmpty())
                    outputs.computeIfAbsent(item.getItem(), k -> new ArrayList<>()).add(recipe);
        }
    }

    public List<Recipe<?>> all() {
        return all;
    }

    public List<Recipe<?>> byInput(Collection<ItemStack> stacks) {
        var candidates = new LinkedHashSet<Recipe<?>>();
        for (var stack : stacks) {
            if (stack.isEmpty()) continue;
            candidates.addAll(inputs.getOrDefault(stack.getItem(), List.of()));
            if (!fluids.isEmpty())
                FluidUtil.getFluidHandler(stack.copy())
                        .ifPresent(
                                handler -> {
                                    for (int tank = 0; tank < handler.getTanks(); tank++)
                                        candidates.addAll(
                                                fluids.getOrDefault(
                                                        handler.getFluidInTank(tank).getFluid(),
                                                        List.of()));
                                });
        }
        candidates.addAll(wildcard);
        return candidates.stream()
                .sorted(
                        Comparator.<Recipe<?>>comparingInt(r -> ((RecipeDefinition) r).priority())
                                .reversed()
                                .thenComparing(r -> r.getId().toString()))
                .toList();
    }

    public List<Recipe<?>> byOutput(ItemStack stack) {
        return List.copyOf(outputs.getOrDefault(stack.getItem(), List.of()));
    }
}
