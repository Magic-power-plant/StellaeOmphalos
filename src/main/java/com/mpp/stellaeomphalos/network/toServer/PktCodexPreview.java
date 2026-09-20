package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

public record PktCodexPreview(ResourceLocation blueprint) implements OmphalosPayload {
    public static final Codec<PktCodexPreview> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("blueprint")
                                                    .forGetter(PktCodexPreview::blueprint))
                                    .apply(i, PktCodexPreview::new));
}
