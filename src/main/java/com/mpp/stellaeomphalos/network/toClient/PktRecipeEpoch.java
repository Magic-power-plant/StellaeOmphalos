package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record PktRecipeEpoch(int epoch, List<ResourceLocation> disabledFamilies)
        implements OmphalosPayload {
    public static final Codec<PktRecipeEpoch> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("epoch")
                                                    .forGetter(PktRecipeEpoch::epoch),
                                            ResourceLocation.CODEC
                                                    .listOf()
                                                    .fieldOf("disabled_families")
                                                    .forGetter(PktRecipeEpoch::disabledFamilies))
                                    .apply(instance, PktRecipeEpoch::new));

    public PktRecipeEpoch {
        if (disabledFamilies.size() > 128)
            throw new IllegalArgumentException("Too many disabled families");
        disabledFamilies = List.copyOf(disabledFamilies);
    }
}
