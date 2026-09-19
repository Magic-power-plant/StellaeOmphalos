package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

public record PktConfigVersion(int version) implements OmphalosPayload {
    public static final Codec<PktConfigVersion> CODEC = Codec.intRange(0, Integer.MAX_VALUE).fieldOf("version")
            .xmap(PktConfigVersion::new, PktConfigVersion::version).codec();
}
