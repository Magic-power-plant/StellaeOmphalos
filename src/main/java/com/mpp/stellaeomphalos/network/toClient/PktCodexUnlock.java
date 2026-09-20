package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import java.util.List;

/** Append-only payload 44; business validation belongs to the receiving domain. */
public record PktCodexUnlock(List<String> nodes) implements OmphalosPayload {
    public static final Codec<PktCodexUnlock> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.STRING
                                                    .listOf()
                                                    .fieldOf("nodes")
                                                    .forGetter(PktCodexUnlock::nodes))
                                    .apply(i, PktCodexUnlock::new));

    public PktCodexUnlock {
        nodes = List.copyOf(nodes);
    }
}
