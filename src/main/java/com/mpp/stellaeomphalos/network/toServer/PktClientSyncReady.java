package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

public record PktClientSyncReady(String protocol) implements OmphalosPayload {
    public static final Codec<PktClientSyncReady> CODEC = com.mpp.stellaeomphalos.data.codec.FoundationCodecs.optional(Codec.STRING,
            "protocol", "1.0").xmap(PktClientSyncReady::new, PktClientSyncReady::protocol).codec();
    public PktClientSyncReady() { this(com.mpp.stellaeomphalos.network.ProtocolVersion.CURRENT.toString()); }
}
