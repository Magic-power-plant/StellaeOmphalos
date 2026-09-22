package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Named route for handheld and scroll screens, no numeric GUI dispatch. */
public record PktOpenObservation(
        String route, Optional<ResourceLocation> sign, java.util.List<ResourceLocation> signs)
        implements OmphalosPayload {
    public PktOpenObservation(String route, Optional<ResourceLocation> sign) {
        this(route, sign, java.util.List.of());
    }

    public PktOpenObservation {
        signs = java.util.List.copyOf(signs);
        if (signs.size() > 128) throw new IllegalArgumentException("Too many scroll signs");
    }

    public static final Codec<PktOpenObservation> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.STRING
                                                    .fieldOf("route")
                                                    .forGetter(PktOpenObservation::route),
                                            ResourceLocation.CODEC
                                                    .optionalFieldOf("sign")
                                                    .forGetter(PktOpenObservation::sign),
                                            ResourceLocation.CODEC
                                                    .listOf()
                                                    .optionalFieldOf("signs", java.util.List.of())
                                                    .forGetter(PktOpenObservation::signs))
                                    .apply(i, PktOpenObservation::new));
}
