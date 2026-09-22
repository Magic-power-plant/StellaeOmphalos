package com.mpp.stellaeomphalos.client.codex;

/** A landscape spread contains two logical 175x220 pages. */
public final class CodexLayout {
    public static final int WIDTH = 430,
            HEIGHT = 304,
            // Visible book art width; side ribbons overhang it symmetrically, so the shell is
            // centered on this rather than on the ribbon-inclusive WIDTH.
            BOOK_WIDTH = 393,
            PAGE_WIDTH = 175,
            PAGE_HEIGHT = 220,
            LEFT = 20,
            RIGHT = 210,
            TOP = 24,
            RIBBON_X = 395,
            RIBBON_STEP = 28;

    private CodexLayout() {}
}
