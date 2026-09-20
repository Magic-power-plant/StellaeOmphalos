package com.mpp.stellaeomphalos.ritual.amplifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record AmplifierTier(
        int rank,
        float multiplier,
        int maxPerSlot,
        List<ResourceLocation> compatibleSigns,
        boolean consumesOnCycle,
        int lumenUpkeepPerTick) {
    public static final Codec<AmplifierTier> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.intRange(0, 32)
                                                    .fieldOf("rank")
                                                    .forGetter(AmplifierTier::rank),
                                            Codec.floatRange(0, 16)
                                                    .fieldOf("multiplier")
                                                    .forGetter(AmplifierTier::multiplier),
                                            Codec.intRange(1, 64)
                                                    .fieldOf("max_per_slot")
                                                    .forGetter(AmplifierTier::maxPerSlot),
                                            ResourceLocation.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("compatible_signs", List.of())
                                                    .forGetter(AmplifierTier::compatibleSigns),
                                            Codec.BOOL
                                                    .optionalFieldOf("consumes_on_cycle", false)
                                                    .forGetter(AmplifierTier::consumesOnCycle),
                                            Codec.intRange(0, 10000)
                                                    .optionalFieldOf("lumen_upkeep_per_tick", 0)
                                                    .forGetter(AmplifierTier::lumenUpkeepPerTick))
                                    .apply(i, AmplifierTier::new));

    public AmplifierTier {
        compatibleSigns = List.copyOf(compatibleSigns);
    }

    public boolean compatible(ResourceLocation sign) {
        return compatibleSigns.isEmpty() || compatibleSigns.contains(sign);
    }

    public double factor(ResourceLocation sign) {
        return compatible(sign)
                ? multiplier * (compatibleSigns.isEmpty() ? 1 : 1.25) * (consumesOnCycle ? 1 : 0.9)
                : 1;
    }

    public static float intensity(
            float base, double multiplier, double degradation, int interrupts) {
        return (float)
                Math.max(
                        0,
                        Math.min(
                                2,
                                base
                                        * multiplier
                                        * (1 - 0.15 * Math.max(0, Math.min(1, degradation)))
                                        * (1 - 0.1 * Math.min(5, interrupts))));
    }
}
