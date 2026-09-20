package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

/** Append-only payload 41; business validation belongs to the receiving domain. */
public record PktShardRevealed(String shard, int hand, String result) implements OmphalosPayload {
    public static final Codec<PktShardRevealed> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.STRING
                                                    .fieldOf("shard")
                                                    .forGetter(PktShardRevealed::shard),
                                            Codec.intRange(0, 1)
                                                    .fieldOf("hand")
                                                    .forGetter(PktShardRevealed::hand),
                                            Codec.STRING
                                                    .fieldOf("result")
                                                    .forGetter(PktShardRevealed::result))
                                    .apply(i, PktShardRevealed::new));
}
