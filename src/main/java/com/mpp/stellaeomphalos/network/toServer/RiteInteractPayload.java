package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;

import java.util.*;

public record RiteInteractPayload(BlockPos origin, int action, int slot)
        implements OmphalosPayload {
    public static final Codec<RiteInteractPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(RiteInteractPayload::origin),
                                            Codec.intRange(0, 2)
                                                    .fieldOf("action")
                                                    .forGetter(RiteInteractPayload::action),
                                            Codec.intRange(0, 1)
                                                    .fieldOf("slot")
                                                    .forGetter(RiteInteractPayload::slot))
                                    .apply(i, RiteInteractPayload::new));
}
