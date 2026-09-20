package com.mpp.stellaeomphalos.constellation.starmap;

/** Immutable grid coordinate of a single star; the drawing grid is {@link #GRID} cells wide. */
public record StarPoint(int x, int y) {
    public static final int GRID = 31;
    public StarPoint {
        if (x < 0 || x >= GRID || y < 0 || y >= GRID)
            throw new IllegalArgumentException("Star coordinate outside [0," + (GRID - 1) + "]: " + x + "," + y);
    }
    /** Manhattan distance to the grid origin, used for sorting and heuristics only. */
    public int manhattan() { return Math.abs(x) + Math.abs(y); }
}
