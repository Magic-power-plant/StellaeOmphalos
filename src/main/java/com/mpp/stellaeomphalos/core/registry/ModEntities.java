package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;

public final class ModEntities {
    public static final RegistryFamily<EntityType<?>> ENTRIES = new RegistryFamily<>(Registries.ENTITY_TYPE);
    private ModEntities() {}
}
