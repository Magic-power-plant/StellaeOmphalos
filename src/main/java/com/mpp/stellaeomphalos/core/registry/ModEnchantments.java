package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.core.registries.Registries;

public final class ModEnchantments {
    public static final RegistryFamily<Enchantment> ENTRIES = new RegistryFamily<>(Registries.ENCHANTMENT);
    private ModEnchantments() {}
}
