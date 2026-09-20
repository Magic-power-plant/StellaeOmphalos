package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

/** Append-only payload 42; business validation belongs to the receiving domain. */
public record PktRevealShard(int hand) implements OmphalosPayload {
    public static final Codec<PktRevealShard> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.intRange(0, 1)
                                                    .fieldOf("hand")
                                                    .forGetter(PktRevealShard::hand))
                                    .apply(i, PktRevealShard::new));
}
