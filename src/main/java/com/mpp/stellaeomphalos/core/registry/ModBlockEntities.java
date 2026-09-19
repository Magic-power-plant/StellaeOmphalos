package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.Registries;

public final class ModBlockEntities {
    public static final RegistryFamily<BlockEntityType<?>> ENTRIES = new RegistryFamily<>(Registries.BLOCK_ENTITY_TYPE);
    private ModBlockEntities() {}
}
