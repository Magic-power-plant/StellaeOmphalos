package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** The eight exact symmetries of the horizontal square, mirror first then rotate. */
public enum PlacementTransform {
    NONE(0, false),
    CLOCKWISE_90(1, false),
    CLOCKWISE_180(2, false),
    COUNTERCLOCKWISE_90(3, false),
    MIRRORED_NONE(0, true),
    MIRRORED_CLOCKWISE_90(1, true),
    MIRRORED_180(2, true),
    MIRRORED_CCW_90(3, true);
    private final int turns;
    private final boolean mirrored;

    PlacementTransform(int turns, boolean mirrored) {
        this.turns = turns;
        this.mirrored = mirrored;
    }

    public boolean mirrored() {
        return mirrored;
    }

    public PlacementTransform withoutMirror() {
        return values()[turns];
    }

    public BlockPos apply(BlockPos p) {
        int x = p.getX(), z = mirrored ? -p.getZ() : p.getZ();
        for (int n = 0; n < turns; n++) {
            int old = x;
            x = -z;
            z = old;
        }
        return new BlockPos(x, p.getY(), z);
    }

    public BlockState apply(BlockState s) {
        return s.mirror(mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE)
                .rotate(Rotation.values()[turns]);
    }

    public PlacementTransform compose(PlacementTransform other) {
        for (var t : values())
            if (t.apply(new BlockPos(1, 0, 0)).equals(apply(other.apply(new BlockPos(1, 0, 0))))
                    && t.apply(new BlockPos(0, 0, 1))
                            .equals(apply(other.apply(new BlockPos(0, 0, 1))))) return t;
        throw new IllegalStateException("Non-square transform");
    }

    public PlacementTransform inverse() {
        for (var t : values()) if (compose(t) == NONE) return t;
        throw new IllegalStateException();
    }

    public static PlacementTransform of(Rotation r, Mirror m) {
        var base = values()[r.ordinal()];
        return m == Mirror.NONE
                ? base
                : m == Mirror.LEFT_RIGHT ? base.compose(MIRRORED_NONE) : base.compose(MIRRORED_180);
    }
}
