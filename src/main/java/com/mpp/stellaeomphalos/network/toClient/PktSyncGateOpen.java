package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

public record PktSyncGateOpen(String protocol) implements OmphalosPayload {
    public static final Codec<PktSyncGateOpen> CODEC = Codec.STRING.fieldOf("protocol").xmap(PktSyncGateOpen::new, PktSyncGateOpen::protocol).codec();
}
