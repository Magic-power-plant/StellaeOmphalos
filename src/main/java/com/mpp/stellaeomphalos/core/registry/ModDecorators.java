package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.core.registries.Registries;

public final class ModDecorators {
    public static final RegistryFamily<TreeDecoratorType<?>> ENTRIES = new RegistryFamily<>(Registries.TREE_DECORATOR_TYPE);
    private ModDecorators() {}
}
