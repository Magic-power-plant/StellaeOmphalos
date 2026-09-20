package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record PreviewEndPayload(ResourceLocation blueprintId) implements OmphalosPayload {
    public static final Codec<PreviewEndPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("blueprintId")
                                                    .forGetter(PreviewEndPayload::blueprintId))
                                    .apply(i, PreviewEndPayload::new));
}
