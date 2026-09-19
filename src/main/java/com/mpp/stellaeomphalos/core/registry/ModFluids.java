package com.mpp.stellaeomphalos.core.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModFluids {
    public static final RegistryFamily<Fluid> FLUIDS = new RegistryFamily<>(Registries.FLUID);
    public static final RegistryFamily<FluidType> TYPES = new RegistryFamily<>(ForgeRegistries.Keys.FLUID_TYPES);
    private ModFluids() {}
}
