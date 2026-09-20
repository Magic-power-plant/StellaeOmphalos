package com.mpp.stellaeomphalos.content.world;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

public record SpringFluidEntry(
        ResourceLocation fluid,
        int guaranteedMb,
        int extraRandomMb,
        double weight,
        String requiredMod,
        String temperatureClass) {
    public static final Codec<SpringFluidEntry> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                            .fieldOf("fluid")
                                                            .forGetter(SpringFluidEntry::fluid),
                                                    Codec.intRange(0, 100000000)
                                                            .fieldOf("guaranteed_mb")
                                                            .forGetter(
                                                                    SpringFluidEntry::guaranteedMb),
                                            Codec.intRange(0, 100000000)
                                                            .optionalFieldOf("extra_random_mb", 0)
                                                            .forGetter(
                                                                    SpringFluidEntry
                                                                            ::extraRandomMb),
                                                    Codec.doubleRange(0, 1000000)
                                                            .fieldOf("weight")
                                                            .forGetter(SpringFluidEntry::weight),
                                            Codec.STRING
                                                            .optionalFieldOf("required_mod", "")
                                                            .forGetter(
                                                                    SpringFluidEntry::requiredMod),
                                                    Codec.STRING
                                                            .optionalFieldOf(
                                                                    "temperature_class", "ANY")
                                                            .forGetter(
                                                                    SpringFluidEntry
                                                                            ::temperatureClass))
                                    .apply(i, SpringFluidEntry::new));

    public SpringFluidEntry {
        if (!java.util.Set.of("COLD", "WARM", "HOT", "ANY").contains(temperatureClass))
            throw new IllegalArgumentException("Temperature class");
    }
}
