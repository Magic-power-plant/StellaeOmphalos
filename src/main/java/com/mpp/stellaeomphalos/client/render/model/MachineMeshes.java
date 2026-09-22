package com.mpp.stellaeomphalos.client.render.model;

/** All eleven authored part definitions live here; animations alter the pose, never the mesh. */
public final class MachineMeshes {
    public static final PartMesh ALTAR_RESONANCE = mesh(0, 2, 0, 10, 4, 10, 0, 6, 0, 5, 4, 5);
    public static final PartMesh ALTAR_TIER_TWO = mesh(0, 1, 0, 3, 2, 8, 0, 3, -2, 2, 2, 4);
    public static final PartMesh ALTAR_TIER_THREE = mesh(0, 1, 0, 3, 2, 10, 0, 3, 0, 2, 2, 6);
    public static final PartMesh MANTLE_ARMOR = mesh(0, 4, 1, 10, 8, 2, 0, 12, 2, 12, 8, 2);
    public static final PartMesh GRINDSTONE = mesh(0, 0, 0, 3, 10, 10, 0, 0, 0, 7, 2, 2);
    public static final PartMesh LENS = mesh(0, 8, 0, 2, 10, 10, 0, 2, 0, 4, 4, 4);
    public static final PartMesh LENS_COLOR = mesh(0, 8, 0, 2.1F, 7, 7);
    public static final PartMesh OBSERVATORY = mesh(0, 0, 0, 5, 5, 22, 0, 0, -12, 7, 7, 2);
    public static final PartMesh PRISM_COLOR =
            mesh(0, 5, 0, 7, 1, 7, 0, 11, 0, 7, 1, 7, -3, 8, -3, 1, 6, 1, 3, 8, 3, 1, 6, 1);
    public static final PartMesh STAR_CHART_TABLE =
            mesh(0, 12, 0, 14, 2, 14, -5, 6, -5, 2, 12, 2, 5, 6, 5, 2, 12, 2);
    public static final PartMesh TELESCOPE = mesh(0, 0, 0, 3, 3, 16, 0, 0, -8, 5, 5, 2);

    private MachineMeshes() {}

    private static PartMesh mesh(float... boxes) {
        return new PartMesh(boxes);
    }
}
