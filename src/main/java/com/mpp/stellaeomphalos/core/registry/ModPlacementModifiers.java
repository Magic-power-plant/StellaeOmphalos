package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.core.registries.Registries;

public final class ModPlacementModifiers {
    public static final RegistryFamily<PlacementModifierType<?>> ENTRIES = new RegistryFamily<>(Registries.PLACEMENT_MODIFIER_TYPE);
    private ModPlacementModifiers() {}
}
