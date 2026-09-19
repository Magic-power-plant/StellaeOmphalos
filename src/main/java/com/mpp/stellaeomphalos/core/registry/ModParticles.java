package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;

public final class ModParticles {
    public static final RegistryFamily<ParticleType<?>> ENTRIES = new RegistryFamily<>(Registries.PARTICLE_TYPE);
    private ModParticles() {}
}
