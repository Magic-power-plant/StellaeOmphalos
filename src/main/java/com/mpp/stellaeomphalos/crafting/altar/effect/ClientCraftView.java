package com.mpp.stellaeomphalos.crafting.altar.effect;

import com.mpp.stellaeomphalos.crafting.altar.recipe.*;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public record ClientCraftView(
        BlockPos position,
        ResourceLocation recipe,
        AsterismTier tier,
        CraftState state,
        double progress,
        Map<Integer, BlockPos> bindings,
        List<ItemStack> unboundRequirements) {
    public ClientCraftView {
        position = position.immutable();
        bindings = Map.copyOf(bindings);
        unboundRequirements = unboundRequirements.stream().map(ItemStack::copy).toList();
    }

    @Override
    public List<ItemStack> unboundRequirements() {
        return unboundRequirements.stream().map(ItemStack::copy).toList();
    }

    public boolean isActive() {
        return state == CraftState.RUNNING;
    }
}
