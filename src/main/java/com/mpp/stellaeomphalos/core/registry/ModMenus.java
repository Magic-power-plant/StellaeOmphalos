package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.registries.Registries;

public final class ModMenus {
    public static final RegistryFamily<MenuType<?>> ENTRIES = new RegistryFamily<>(Registries.MENU);
    private ModMenus() {}
}
