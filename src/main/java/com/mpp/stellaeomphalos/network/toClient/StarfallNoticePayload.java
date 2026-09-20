package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record StarfallNoticePayload(ResourceLocation dimension, double x, double z, int azimuth)
        implements OmphalosPayload {
    public static final Codec<StarfallNoticePayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("dimension")
                                                    .forGetter(StarfallNoticePayload::dimension),
                                            Codec.DOUBLE
                                                    .fieldOf("x")
                                                    .forGetter(StarfallNoticePayload::x),
                                            Codec.DOUBLE
                                                    .fieldOf("z")
                                                    .forGetter(StarfallNoticePayload::z),
                                            Codec.intRange(0, 3600)
                                                    .fieldOf("azimuth")
                                                    .forGetter(StarfallNoticePayload::azimuth))
                                    .apply(i, StarfallNoticePayload::new));
}
