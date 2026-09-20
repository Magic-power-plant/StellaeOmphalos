package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

/** Append-only payload 49; business validation belongs to the receiving domain. */
public record PktKnowledgeQuery(String kind) implements OmphalosPayload {
    public static final Codec<PktKnowledgeQuery> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(Codec.STRING.fieldOf("kind").forGetter(PktKnowledgeQuery::kind))
                                    .apply(i, PktKnowledgeQuery::new));
}
