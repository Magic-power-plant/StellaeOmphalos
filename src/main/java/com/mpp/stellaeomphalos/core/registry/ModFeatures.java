package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.core.registries.Registries;

public final class ModFeatures {
    public static final RegistryFamily<Feature<?>> ENTRIES = new RegistryFamily<>(Registries.FEATURE);
    private ModFeatures() {}
}
