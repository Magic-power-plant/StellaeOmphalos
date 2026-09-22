package com.mpp.stellaeomphalos.client.render.res;

import net.minecraft.resources.ResourceLocation;

/** Immutable UV descriptor. Sampling uses monotonic wall time, independent of game pause. */
public record SpriteStrip(ResourceLocation location, int rows, int cols, int frameMillis) {
    public SpriteStrip {
        if (rows < 1 || cols < 1 || frameMillis < 1)
            throw new IllegalArgumentException("Invalid strip");
    }

    public int frame(long millis) {
        return (int) Math.floorMod(millis / frameMillis, (long) rows * cols);
    }

    public float u0(long millis) {
        return (float) (frame(millis) % cols) / cols;
    }

    public float v0(long millis) {
        return (float) (frame(millis) / cols) / rows;
    }

    public float u1(long millis) {
        return u0(millis) + 1F / cols;
    }

    public float v1(long millis) {
        return v0(millis) + 1F / rows;
    }
}
