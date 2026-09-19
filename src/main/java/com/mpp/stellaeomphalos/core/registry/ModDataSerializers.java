package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModDataSerializers {
    public static final RegistryFamily<EntityDataSerializer<?>> ENTRIES = new RegistryFamily<>(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS);
    private ModDataSerializers() {}
}
