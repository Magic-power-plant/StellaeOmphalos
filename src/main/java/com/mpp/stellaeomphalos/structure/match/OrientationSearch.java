package com.mpp.stellaeomphalos.structure.match;

import com.mpp.stellaeomphalos.structure.pattern.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Incremental orientation discovery; the matching authority still verifies a completed result. */
final class OrientationSearch extends AbstractStructureWatch {
    private final List<BlockPlacement> cells;
    private final List<PlacementTransform> transforms;
    private int transformIndex, cursor;
    private PlacementTransform result;
    OrientationSearch(BlockPos origin, BuildBlueprint blueprint) {
        super(origin, blueprint.id());
        cells = List.copyOf(blueprint.blocks().values());
        transforms = Arrays.stream(PlacementTransform.values()).filter(t -> blueprint.mirrorable() || !t.mirrored()).toList();
    }
    int advance(ServerLevel level, int budget) {
        int used = 0;
        while (!done() && used < budget) {
            if (cursor == cells.size()) { result = transforms.get(transformIndex); break; }
            var transform = transforms.get(transformIndex);
            var cell = cells.get(cursor++);
            var pos = origin.offset(transform.apply(cell.relative()));
            used++;
            if (!level.hasChunkAt(pos) || cell.severity() == MismatchSeverity.REQUIRED
                    && !cell.rule().transform(transform).matches(level.getBlockState(pos))) {
                cursor = 0; transformIndex++;
            }
        }
        if (!done() && cursor == cells.size()) result = transforms.get(transformIndex);
        return used;
    }
    @Override public StructureState state() {
        return closed() ? StructureState.LOCKED : result != null ? StructureState.FORMED
                : done() ? StructureState.BROKEN : StructureState.INDETERMINATE;
    }
    boolean done() { return closed() || result != null || transformIndex >= transforms.size(); }
    Optional<PlacementTransform> result() { return Optional.ofNullable(result); }
    @Override protected void detach() { result = null; }
}
