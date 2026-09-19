package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.core.BlockPos;

public record PktNetworkDebugDumpRequest(BlockPos position) implements OmphalosPayload {
    public static final Codec<PktNetworkDebugDumpRequest> CODEC = BlockPos.CODEC.fieldOf("position")
            .xmap(PktNetworkDebugDumpRequest::new, PktNetworkDebugDumpRequest::position).codec();
    public PktNetworkDebugDumpRequest { position = position.immutable(); }
}
