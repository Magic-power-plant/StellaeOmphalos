package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

/** Append-only payload 47; business validation belongs to the receiving domain. */
public record PktCodexRead(String route) implements OmphalosPayload {
    public static final Codec<PktCodexRead> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(Codec.STRING.fieldOf("route").forGetter(PktCodexRead::route))
                                    .apply(i, PktCodexRead::new));
}
