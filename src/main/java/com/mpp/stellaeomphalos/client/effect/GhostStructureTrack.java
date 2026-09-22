package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.render.DeferredEffectQueue;
import com.mpp.stellaeomphalos.client.render.PhantomBatch;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.structure.preview.PreviewSession;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.BooleanSupplier;

/** Functional preview is mandatory. Geometry and hit/culling bounds are rebuilt only on tick. */
public final class GhostStructureTrack extends AbstractEffectTrack {
    private record Cell(BlockPos pos, BlockState state, AABB bounds, int tint) {}

    private final PreviewSession preview;
    private final BooleanSupplier current;
    private Cell[] cells = new Cell[0];
    private int revision = -1, resourceEpoch = -1;

    public GhostStructureTrack(PreviewSession preview, int lifetime, BooleanSupplier current) {
        super(EffectLane.GHOST, lifetime, 100, true);
        this.preview = preview;
        this.current = current;
    }

    @Override
    protected void advance() {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !current.getAsBoolean()) {
            expire();
            return;
        }
        if (revision == preview.revision()
                && resourceEpoch == PhantomBatch.epoch()
                && age % 10 != 0) return;
        revision = preview.revision();
        resourceEpoch = PhantomBatch.epoch();
        var next = new java.util.ArrayList<Cell>();
        for (var entry : preview.cells().entrySet()) {
            var p = preview.origin().offset(BlockPos.of(entry.getKey()));
            if (!mc.level.hasChunkAt(p)) continue;
            var state = Block.stateById(entry.getValue());
            if (state.isAir()) continue;
            PhantomBatch.prepare(state);
            int color = mc.level.getBlockState(p).isAir() ? 0x5584c5e5 : 0x77ff6262;
            next.add(new Cell(p, state, new AABB(p), color));
        }
        cells = next.toArray(Cell[]::new);
    }

    public double distanceSq(double x, double y, double z) {
        return preview.origin().distToCenterSqr(x, y, z);
    }

    public void collect(WorldDraw draw) {
        for (int i = 0; i < cells.length && draw.vertices < 250000; i++) {
            var cell = cells[i];
            if (!DeferredEffectQueue.visible(cell.bounds)) continue;
            draw.pose.pushPose();
            try {
                draw.pose.translate(cell.pos.getX() + .5, cell.pos.getY(), cell.pos.getZ() + .5);
                PhantomBatch.draw(cell.state, draw, cell.tint);
            } finally {
                draw.pose.popPose();
            }
        }
    }
}
