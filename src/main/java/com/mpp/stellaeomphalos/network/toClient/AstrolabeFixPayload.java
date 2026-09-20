package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record AstrolabeFixPayload(
        ResourceLocation targetId,
        int bearingQ,
        int altitudeQ,
        int distanceBand,
        int precision,
        int deniedReason,
        Optional<BlockPos> exact)
        implements OmphalosPayload {
    public static final Codec<AstrolabeFixPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("targetId")
                                                    .forGetter(AstrolabeFixPayload::targetId),
                                            Codec.intRange(-3600, 3600)
                                                    .fieldOf("bearingQ")
                                                    .forGetter(AstrolabeFixPayload::bearingQ),
                                            Codec.intRange(-900, 900)
                                                    .fieldOf("altitudeQ")
                                                    .forGetter(AstrolabeFixPayload::altitudeQ),
                                            Codec.intRange(0, 65536)
                                                    .fieldOf("distanceBand")
                                                    .forGetter(AstrolabeFixPayload::distanceBand),
                                            Codec.intRange(0, 2)
                                                    .fieldOf("precision")
                                                    .forGetter(AstrolabeFixPayload::precision),
                                            Codec.intRange(0, 8)
                                                    .fieldOf("deniedReason")
                                                    .forGetter(AstrolabeFixPayload::deniedReason),
                                            BlockPos.CODEC
                                                    .optionalFieldOf("exact")
                                                    .codec()
                                                    .fieldOf("exact")
                                                    .forGetter(AstrolabeFixPayload::exact))
                                    .apply(i, AstrolabeFixPayload::new));

    public AstrolabeFixPayload {
        if (precision < 2 && exact.isPresent())
            throw new IllegalArgumentException("Coarse fix leaks position");
    }
}
