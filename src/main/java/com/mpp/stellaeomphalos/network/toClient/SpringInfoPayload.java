package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import java.util.*;

public record SpringInfoPayload(
        int chunkX, int chunkZ, boolean present, int temperatureClass, int fillBand)
        implements OmphalosPayload {
    public static final Codec<SpringInfoPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("chunkX")
                                                    .forGetter(SpringInfoPayload::chunkX),
                                            Codec.INT
                                                    .fieldOf("chunkZ")
                                                    .forGetter(SpringInfoPayload::chunkZ),
                                            Codec.BOOL
                                                    .fieldOf("present")
                                                    .forGetter(SpringInfoPayload::present),
                                            Codec.intRange(0, 3)
                                                    .fieldOf("temperatureClass")
                                                    .forGetter(SpringInfoPayload::temperatureClass),
                                            Codec.intRange(0, 4)
                                                    .fieldOf("fillBand")
                                                    .forGetter(SpringInfoPayload::fillBand))
                                    .apply(i, SpringInfoPayload::new));
}
