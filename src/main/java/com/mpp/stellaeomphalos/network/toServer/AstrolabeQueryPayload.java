package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record AstrolabeQueryPayload(ResourceLocation targetId) implements OmphalosPayload {
    public static final Codec<AstrolabeQueryPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("targetId")
                                                    .forGetter(AstrolabeQueryPayload::targetId))
                                    .apply(i, AstrolabeQueryPayload::new));
}
