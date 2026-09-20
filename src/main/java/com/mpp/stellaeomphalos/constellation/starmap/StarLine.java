package com.mpp.stellaeomphalos.constellation.starmap;

/** Undirected edge between two star points; endpoints are normalized so equals/hashCode ignore direction. */
public record StarLine(StarPoint a, StarPoint b) {
    public StarLine {
        if (a.equals(b)) throw new IllegalArgumentException("Self connection " + a);
        if (compare(a, b) > 0) { var swap = a; a = b; b = swap; }
    }
    private static int compare(StarPoint first, StarPoint second) {
        int byX = Integer.compare(first.x(), second.x());
        return byX != 0 ? byX : Integer.compare(first.y(), second.y());
    }
}
