package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;

import java.util.*;

public record StructureStatePayload(BlockPos origin, int state, int formedPercent, int degradations)
        implements OmphalosPayload {
    public static final Codec<StructureStatePayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(StructureStatePayload::origin),
                                            Codec.intRange(0, 4)
                                                    .fieldOf("state")
                                                    .forGetter(StructureStatePayload::state),
                                            Codec.intRange(0, 100)
                                                    .fieldOf("formedPercent")
                                                    .forGetter(
                                                            StructureStatePayload::formedPercent),
                                            Codec.intRange(0, 65536)
                                                    .fieldOf("degradations")
                                                    .forGetter(StructureStatePayload::degradations))
                                    .apply(i, StructureStatePayload::new));
}
