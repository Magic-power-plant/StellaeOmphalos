package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record PktGatewayTargets(
        ResourceLocation dimension, BlockPos origin, long token, List<BlockPos> targets)
        implements OmphalosPayload {
    public PktGatewayTargets {
        targets = List.copyOf(targets);
    }

    public static final Codec<PktGatewayTargets> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("dimension")
                                                    .forGetter(PktGatewayTargets::dimension),
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(PktGatewayTargets::origin),
                                            Codec.LONG
                                                    .fieldOf("token")
                                                    .forGetter(PktGatewayTargets::token),
                                            BlockPos.CODEC
                                                    .listOf()
                                                    .flatXmap(
                                                            v ->
                                                                    v.size() <= 64
                                                                            ? DataResult.success(v)
                                                                            : DataResult.error(
                                                                                    () ->
                                                                                            "Too many"
                                                                                                + " gateways"),
                                                            DataResult::success)
                                                    .fieldOf("targets")
                                                    .forGetter(PktGatewayTargets::targets))
                                    .apply(i, PktGatewayTargets::new));
}
