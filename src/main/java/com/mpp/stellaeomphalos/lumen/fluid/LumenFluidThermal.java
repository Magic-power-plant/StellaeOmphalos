package com.mpp.stellaeomphalos.lumen.fluid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Fluid temperature registry. Explicit registrations win; unregistered fluids fall back to their
 * FluidType temperature (vanilla water/lava carry explicit fallbacks below). Third parties may
 * register additional temperatures at any time.
 */
public final class LumenFluidThermal {
    public static final int COLD_THRESHOLD = 300;
    private static final Map<ResourceLocation, Integer> TEMPERATURES = new ConcurrentHashMap<>();

    static {
        register(new ResourceLocation("minecraft", "water"), 300);
        register(new ResourceLocation("minecraft", "flowing_water"), 300);
        register(new ResourceLocation("minecraft", "lava"), 1300);
        register(new ResourceLocation("minecraft", "flowing_lava"), 1300);
    }

    private LumenFluidThermal() {}

    public static void register(ResourceLocation fluidId, int temperature) { TEMPERATURES.put(fluidId, temperature); }

    public static int temperatureOf(FluidState state) {
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(state.getType());
        Integer registered = id == null ? null : TEMPERATURES.get(id);
        return registered != null ? registered : state.getFluidType().getTemperature();
    }
}
