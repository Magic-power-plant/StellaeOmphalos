package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Side-neutral camera protocol. No entity identity or player position is modified by the consumer.
 */
public record PktViewSequence(ResourceLocation dimension, int priority, List<Point> points)
        implements OmphalosPayload {
    public record Point(double x, double y, double z, int tick, float yaw, float pitch, float fov) {
        public static final Codec<Point> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.doubleRange(-30000000, 30000000)
                                                        .fieldOf("x")
                                                        .forGetter(Point::x),
                                                Codec.doubleRange(-2048, 2048)
                                                        .fieldOf("y")
                                                        .forGetter(Point::y),
                                                Codec.doubleRange(-30000000, 30000000)
                                                        .fieldOf("z")
                                                        .forGetter(Point::z),
                                                Codec.intRange(0, 2400)
                                                        .fieldOf("tick")
                                                        .forGetter(Point::tick),
                                                Codec.floatRange(-360, 360)
                                                        .fieldOf("yaw")
                                                        .forGetter(Point::yaw),
                                                Codec.floatRange(-90, 90)
                                                        .fieldOf("pitch")
                                                        .forGetter(Point::pitch),
                                                Codec.floatRange(20, 120)
                                                        .fieldOf("fov")
                                                        .forGetter(Point::fov))
                                        .apply(i, Point::new));
    }

    public PktViewSequence {
        points = List.copyOf(points);
    }

    public static final Codec<PktViewSequence> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("dimension")
                                                    .forGetter(PktViewSequence::dimension),
                                            Codec.intRange(0, 100)
                                                    .fieldOf("priority")
                                                    .forGetter(PktViewSequence::priority),
                                            Point.CODEC
                                                    .listOf()
                                                    .flatXmap(
                                                            PktViewSequence::validate,
                                                            DataResult::success)
                                                    .fieldOf("points")
                                                    .forGetter(PktViewSequence::points))
                                    .apply(i, PktViewSequence::new));

    private static DataResult<List<Point>> validate(List<Point> points) {
        if (points.size() < 2 || points.size() > 128 || points.get(0).tick() != 0)
            return DataResult.error(() -> "Invalid camera path size/origin");
        for (int i = 1; i < points.size(); i++)
            if (points.get(i).tick() <= points.get(i - 1).tick())
                return DataResult.error(() -> "Non-monotonic camera path");
        return DataResult.success(points);
    }
}
