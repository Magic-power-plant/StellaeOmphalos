package com.mpp.stellaeomphalos.crafting.altar.menu;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface SignFocusProvider {
    Optional<ResourceLocation> sign(ItemStack stack);
}
