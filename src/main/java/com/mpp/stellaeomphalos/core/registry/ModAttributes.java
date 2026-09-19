package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.registries.Registries;

public final class ModAttributes {
    public static final RegistryFamily<Attribute> ENTRIES = new RegistryFamily<>(Registries.ATTRIBUTE);
    private ModAttributes() {}
}
