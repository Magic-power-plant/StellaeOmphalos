package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

public record PktSyncGateClose(Reason reason) implements OmphalosPayload {
    public enum Reason { RESYNC, DIMENSION_CHANGE, SERVER_STOP }
    public static final Codec<PktSyncGateClose> CODEC = Codec.STRING.xmap(Reason::valueOf, Reason::name)
            .fieldOf("reason").xmap(PktSyncGateClose::new, PktSyncGateClose::reason).codec();
}
