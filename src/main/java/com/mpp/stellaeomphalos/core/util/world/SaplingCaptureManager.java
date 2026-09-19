package com.mpp.stellaeomphalos.core.util.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;

/** Scope-bound use of Forge's public snapshot API. Only block-only growth actions may be captured. */
public final class SaplingCaptureManager implements AutoCloseable {
    public record Placement(BlockPos relative, BlockState state) {}
    private final ServerLevel level;
    private final BlockPos origin;
    private final int startIndex;
    private boolean closed;
    public SaplingCaptureManager(ServerLevel level, BlockPos origin) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Growth capture off server thread");
        if (level.captureBlockSnapshots || level.restoringBlockSnapshots) throw new IllegalStateException("Nested block snapshot scope");
        this.level = level; this.origin = origin.immutable(); startIndex = level.capturedBlockSnapshots.size();
        level.captureBlockSnapshots = true;
    }
    public List<Placement> snapshot() {
        if (closed) throw new IllegalStateException("Capture closed");
        var changes = new LinkedHashMap<BlockPos, Placement>();
        for (int i = startIndex; i < level.capturedBlockSnapshots.size(); i++) {
            var snapshot = level.capturedBlockSnapshots.get(i);
            changes.put(snapshot.getPos(), new Placement(snapshot.getPos().subtract(origin), snapshot.getCurrentBlock()));
        }
        return List.copyOf(changes.values());
    }
    @Override public void close() {
        if (closed) return;
        closed = true; level.captureBlockSnapshots = false;
        List<BlockSnapshot> snapshots = new ArrayList<>(level.capturedBlockSnapshots.subList(startIndex, level.capturedBlockSnapshots.size()));
        level.capturedBlockSnapshots.subList(startIndex, level.capturedBlockSnapshots.size()).clear();
        boolean previous = level.restoringBlockSnapshots; level.restoringBlockSnapshots = true;
        try {
            RuntimeException failure = null;
            for (int i = snapshots.size() - 1; i >= 0; i--) {
                try {
                    if (!snapshots.get(i).restore(true, false)) throw new IllegalStateException("Cannot restore captured block " + snapshots.get(i).getPos());
                } catch (RuntimeException exception) {
                    if (failure == null) failure = exception; else failure.addSuppressed(exception);
                }
            }
            if (failure != null) throw failure;
        } finally { level.restoringBlockSnapshots = previous; }
    }
    public static boolean replay(ServerLevel level, BlockPos origin, List<Placement> placements) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Growth replay off server thread");
        if (placements.size() > 16384) throw new IllegalArgumentException("Growth snapshot too large");
        for (var placement : placements) {
            var pos = origin.offset(placement.relative());
            if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) return false;
        }
        for (var placement : placements) level.setBlock(origin.offset(placement.relative()), placement.state(), 3);
        return true;
    }
}
