package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;

public record PktGatewayTravel(long token, BlockPos target, boolean cancel)
        implements OmphalosPayload {
    public static final Codec<PktGatewayTravel> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.LONG
                                                    .fieldOf("token")
                                                    .forGetter(PktGatewayTravel::token),
                                            BlockPos.CODEC
                                                    .fieldOf("target")
                                                    .forGetter(PktGatewayTravel::target),
                                            Codec.BOOL
                                                    .fieldOf("cancel")
                                                    .forGetter(PktGatewayTravel::cancel))
                                    .apply(i, PktGatewayTravel::new));
}
