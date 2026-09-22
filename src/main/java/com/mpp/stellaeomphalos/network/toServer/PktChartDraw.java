package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;

import java.util.List;

/** A complete bounded drawing submitted from an open chart-table container. */
public record PktChartDraw(int container, BlockPos origin, List<PktImprintEngrave.Stroke> strokes)
        implements OmphalosPayload {
    public PktChartDraw {
        strokes = List.copyOf(strokes);
    }

    public static final Codec<PktChartDraw> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("container")
                                                    .forGetter(PktChartDraw::container),
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(PktChartDraw::origin),
                                            PktImprintEngrave.Stroke.CODEC
                                                    .listOf()
                                                    .flatXmap(
                                                            v ->
                                                                    v.size() == 3
                                                                            ? DataResult.success(v)
                                                                            : DataResult.error(
                                                                                    () ->
                                                                                            "Chart"
                                                                                                + " requires"
                                                                                                + " three"
                                                                                                + " signs"),
                                                            DataResult::success)
                                                    .fieldOf("strokes")
                                                    .forGetter(PktChartDraw::strokes))
                                    .apply(i, PktChartDraw::new));
}
