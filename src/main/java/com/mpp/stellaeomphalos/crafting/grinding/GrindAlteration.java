package com.mpp.stellaeomphalos.crafting.grinding;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public interface GrindAlteration {
    boolean matches(ItemStack stack);

    GrindOutcome apply(ItemStack stack, RandomSource random);
}
