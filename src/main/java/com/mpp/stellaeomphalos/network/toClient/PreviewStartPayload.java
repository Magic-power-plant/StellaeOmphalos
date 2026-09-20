package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record PreviewStartPayload(
        ResourceLocation blueprintId,
        BlockPos origin,
        int transform,
        int style,
        int ttl,
        long visibilityToken)
        implements OmphalosPayload {
    public static final Codec<PreviewStartPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("blueprintId")
                                                    .forGetter(PreviewStartPayload::blueprintId),
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(PreviewStartPayload::origin),
                                            Codec.intRange(0, 7)
                                                    .fieldOf("transform")
                                                    .forGetter(PreviewStartPayload::transform),
                                            Codec.intRange(0, 3)
                                                    .fieldOf("style")
                                                    .forGetter(PreviewStartPayload::style),
                                            Codec.intRange(1, 12000)
                                                    .fieldOf("ttl")
                                                    .forGetter(PreviewStartPayload::ttl),
                                            Codec.LONG
                                                    .fieldOf("visibilityToken")
                                                    .forGetter(
                                                            PreviewStartPayload::visibilityToken))
                                    .apply(i, PreviewStartPayload::new));
}
