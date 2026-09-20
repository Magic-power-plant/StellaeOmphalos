package com.mpp.stellaeomphalos.core.platform;

import com.mpp.stellaeomphalos.data.codec.MaterialSpec;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.*;

/** Public, read-only recipe browser contract; a slot key is a physical slot, not a list offset. */
public interface RecipeDisplayData {
    ResourceLocation displayCategory();

    List<ResourceLocation> catalysts();

    Map<Integer, MaterialSpec> displaySlots();

    List<ItemStack> displayOutputs();

    default List<FluidStack> displayFluidInputs() {
        return displaySlots().values().stream()
                .flatMap(m -> m.fluidRequirement().stream())
                .flatMap(f -> f.fluids().stream().map(v -> new FluidStack(v, f.amount())))
                .toList();
    }

    default List<FluidStack> displayFluidOutputs() {
        return List.of();
    }

    int displayDuration();

    Map<String, Number> displayScalars();

    boolean hidden();
}
