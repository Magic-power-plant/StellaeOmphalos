package com.mpp.stellaeomphalos.client.render.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

/** Immutable model-space cuboids, in sixteenths, with bottom-centre origin. */
public record PartMesh(float[] boxes) {
    public PartMesh {
        if (boxes.length % 6 != 0) throw new IllegalArgumentException("Six values per cuboid");
        boxes = boxes.clone();
    }

    @Override
    public float[] boxes() {
        return boxes.clone();
    }

    public void emit(WorldDraw draw, VertexConsumer vertices, int color) {
        for (int i = 0; i < boxes.length; i += 6)
            draw.box(
                    vertices,
                    boxes[i] / 16,
                    boxes[i + 1] / 16,
                    boxes[i + 2] / 16,
                    boxes[i + 3] / 32,
                    boxes[i + 4] / 32,
                    boxes[i + 5] / 32,
                    color);
    }
}
