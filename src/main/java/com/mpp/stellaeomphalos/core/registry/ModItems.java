package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;

public final class ModItems {
    public static final RegistryFamily<Item> ENTRIES = new RegistryFamily<>(Registries.ITEM);
    private ModItems() {}
}
