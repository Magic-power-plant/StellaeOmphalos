package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.Registries;

public final class ModEffects {
    public static final RegistryFamily<MobEffect> ENTRIES = new RegistryFamily<>(Registries.MOB_EFFECT);
    private ModEffects() {}
}
