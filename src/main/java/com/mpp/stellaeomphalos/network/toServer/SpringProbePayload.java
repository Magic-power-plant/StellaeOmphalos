package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import java.util.*;

public record SpringProbePayload(int chunkX, int chunkZ) implements OmphalosPayload {
    public static final Codec<SpringProbePayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.intRange(-1875000, 1875000)
                                                    .fieldOf("chunkX")
                                                    .forGetter(SpringProbePayload::chunkX),
                                            Codec.intRange(-1875000, 1875000)
                                                    .fieldOf("chunkZ")
                                                    .forGetter(SpringProbePayload::chunkZ))
                                    .apply(i, SpringProbePayload::new));
}
