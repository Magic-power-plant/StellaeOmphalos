package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.core.registries.Registries;

public final class ModTrunkPlacerTypes {
    public static final RegistryFamily<TrunkPlacerType<?>> ENTRIES = new RegistryFamily<>(Registries.TRUNK_PLACER_TYPE);
    private ModTrunkPlacerTypes() {}
}
