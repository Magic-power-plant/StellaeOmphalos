package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Bounded observation proof; all coordinates and context are rechecked by the server. */
public record PktObserveSign(
        int session,
        ResourceLocation dimension,
        BlockPos origin,
        boolean handheld,
        ResourceLocation sign,
        List<Edge> edges)
        implements OmphalosPayload {
    public record Edge(int ax, int ay, int bx, int by) {
        public static final Codec<Edge> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.intRange(0, 30)
                                                                .fieldOf("ax")
                                                                .forGetter(Edge::ax),
                                                        Codec.intRange(0, 30)
                                                                .fieldOf("ay")
                                                                .forGetter(Edge::ay),
                                                Codec.intRange(0, 30)
                                                                .fieldOf("bx")
                                                                .forGetter(Edge::bx),
                                                        Codec.intRange(0, 30)
                                                                .fieldOf("by")
                                                                .forGetter(Edge::by))
                                        .apply(i, Edge::new));
    }

    public PktObserveSign {
        edges = List.copyOf(edges);
        if (edges.size() > 128) throw new IllegalArgumentException("Too many edges");
    }

    public static final Codec<PktObserveSign> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("session")
                                                    .forGetter(PktObserveSign::session),
                                            ResourceLocation.CODEC
                                                    .fieldOf("dimension")
                                                    .forGetter(PktObserveSign::dimension),
                                            BlockPos.CODEC
                                                    .fieldOf("origin")
                                                    .forGetter(PktObserveSign::origin),
                                            Codec.BOOL
                                                    .fieldOf("handheld")
                                                    .forGetter(PktObserveSign::handheld),
                                            ResourceLocation.CODEC
                                                    .fieldOf("sign")
                                                    .forGetter(PktObserveSign::sign),
                                            Edge.CODEC
                                                    .listOf()
                                                    .flatXmap(
                                                            values ->
                                                                    values.size() <= 128
                                                                            ? DataResult.success(
                                                                                    values)
                                                                            : DataResult.error(
                                                                                    () ->
                                                                                            "Too many"
                                                                                                + " edges"),
                                                            DataResult::success)
                                                    .fieldOf("edges")
                                                    .forGetter(PktObserveSign::edges))
                                    .apply(i, PktObserveSign::new));
}
