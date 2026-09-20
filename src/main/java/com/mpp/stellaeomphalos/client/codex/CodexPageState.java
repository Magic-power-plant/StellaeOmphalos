package com.mpp.stellaeomphalos.client.codex;

import java.util.*;

/** Disposable state shared by page renderers; layout replaces its old lines. */
public final class CodexPageState {
    private int frames;
    private double rotation;
    private int slice = Integer.MAX_VALUE;
    private List<String> lines = List.of();

    public void frame() {
        frames++;
    }

    public boolean rotate(double dx) {
        if (frames <= 30) return false;
        rotation += dx;
        return true;
    }

    public double rotation() {
        return rotation;
    }

    public int slice() {
        return slice;
    }

    public void slice(int value) {
        slice = value;
    }

    public void layout(List<String> next) {
        lines = List.copyOf(next);
    }

    public List<String> lines() {
        return lines;
    }

    public static int candidate(long tick, int row, int column, int count) {
        return count == 0
                ? -1
                : (int) Math.floorMod(Math.floorDiv(tick + row * 40L + column * 40L, 20), count);
    }
}
