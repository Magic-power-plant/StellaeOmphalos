package com.mpp.stellaeomphalos.constellation.starmap;

import net.minecraft.resources.ResourceLocation;

/** One drawn stroke on the drawing table: a sign anchored at a drawing-grid cell. Immutable. */
public record SignDrawn(ResourceLocation sign, int gridX, int gridZ) {
    /** Side length of the square each stroke occupies for hit testing and coverage. */
    public static final int DRAW_SIZE = 30;
}
