package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;

import java.util.*;

public record RiteStatePayload(
        BlockPos origin, int state, int progressPercent, int intensity, int reason)
        implements OmphalosPayload {
    public static final Codec<RiteStatePayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(RiteStatePayload::origin),
                                            Codec.intRange(0, 8)
                                                    .fieldOf("state")
                                                    .forGetter(RiteStatePayload::state),
                                            Codec.intRange(0, 100)
                                                    .fieldOf("progressPercent")
                                                    .forGetter(RiteStatePayload::progressPercent),
                                            Codec.intRange(0, 2000)
                                                    .fieldOf("intensity")
                                                    .forGetter(RiteStatePayload::intensity),
                                            Codec.intRange(0, 8)
                                                    .fieldOf("reason")
                                                    .forGetter(RiteStatePayload::reason))
                                    .apply(i, RiteStatePayload::new));
}
