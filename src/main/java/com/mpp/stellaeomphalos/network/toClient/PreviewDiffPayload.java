package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record PreviewDiffPayload(
        ResourceLocation blueprintId,
        BlockPos origin,
        List<Long> relatives,
        List<Integer> states,
        int totalMissing,
        long visibilityToken,
        boolean reset)
        implements OmphalosPayload {
    public static final Codec<PreviewDiffPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("blueprintId")
                                                    .forGetter(PreviewDiffPayload::blueprintId),
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(PreviewDiffPayload::origin),
                                            Codec.LONG
                                                    .listOf()
                                                    .fieldOf("relatives")
                                                    .forGetter(PreviewDiffPayload::relatives),
                                            Codec.INT
                                                    .listOf()
                                                    .fieldOf("states")
                                                    .forGetter(PreviewDiffPayload::states),
                                            Codec.intRange(0, 65536)
                                                    .fieldOf("totalMissing")
                                                    .forGetter(PreviewDiffPayload::totalMissing),
                                            Codec.LONG
                                                    .fieldOf("visibilityToken")
                                                    .forGetter(PreviewDiffPayload::visibilityToken),
                                            Codec.BOOL
                                                    .fieldOf("reset")
                                                    .forGetter(PreviewDiffPayload::reset))
                                    .apply(i, PreviewDiffPayload::new));

    public PreviewDiffPayload {
        if (relatives.size() > 512 || relatives.size() != states.size())
            throw new IllegalArgumentException("Preview cell limit");
        relatives = List.copyOf(relatives);
        states = List.copyOf(states);
        origin = origin.immutable();
    }
}
