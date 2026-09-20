package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

/** Append-only payload 43; business validation belongs to the receiving domain. */
public record PktOpenCodex(String route) implements OmphalosPayload {
    public static final Codec<PktOpenCodex> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(Codec.STRING.fieldOf("route").forGetter(PktOpenCodex::route))
                                    .apply(i, PktOpenCodex::new));
}
