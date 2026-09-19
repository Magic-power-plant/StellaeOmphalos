package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;

public final class ModCreativeTabs {
    public static final RegistryFamily<CreativeModeTab> ENTRIES = new RegistryFamily<>(Registries.CREATIVE_MODE_TAB);
    private ModCreativeTabs() {}
}
