package com.mpp.stellaeomphalos.structure.pattern;

import com.mpp.stellaeomphalos.structure.match.StructureIntegrityHub;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

/** Transactional preflight prevents partial writes across unloaded chunk boundaries. */
public final class StructurePlacer {
    public record Result(int placed, int skipped, int failed, Set<BlockPos> touched) {
        public Result {
            touched = Set.copyOf(touched);
        }
    }

    private StructurePlacer() {}

    public static Result place(
            BuildBlueprint blueprint,
            ServerLevel level,
            BlockPos origin,
            PlacementTransform requested,
            PlacementContext context) {
        if (blueprint.noPaste() && context.source() == PlacementContext.Source.SCHEMATIC_PASTE)
            return new Result(0, 0, -1, Set.of());
        var transform = blueprint.mirrorable() ? requested : requested.withoutMirror();
        var data = blueprint.transformed(transform);
        var hub = StructureIntegrityHub.of(level);
        for (var p : data.blocks().keySet())
            if (!level.hasChunkAt(origin.offset(p)))
                return new Result(0, data.blocks().size(), 0, Set.of());
        for (var watch : hub.watches())
            if (watch.authority() != null && !watch.origin().equals(origin))
                for (var slot : watch.authority().blueprint().uniqueSlots())
                    for (var own : data.uniqueSlots())
                        if (watch.origin().offset(slot).equals(origin.offset(own)))
                            return new Result(0, 0, -1, Set.of());
        int placed = 0, skipped = 0, failed = 0;
        var touched = new LinkedHashSet<BlockPos>();
        try (var scope = hub.beginBulk()) {
            for (var entry :
                    data.blocks().values().stream()
                            .sorted(Comparator.comparingInt(p -> p.relative().getY()))
                            .toList()) {
                var p = origin.offset(entry.relative());
                var current = level.getBlockState(p);
                if (!context.allowReplaceFluid() && !current.getFluidState().isEmpty()) {
                    skipped++;
                    continue;
                }
                var target = entry.rule().example();
                if (current.equals(target)
                        || level.setBlock(
                                p,
                                target,
                                context.source() == PlacementContext.Source.WORLDGEN ? 2 : 3)) {
                    placed++;
                    touched.add(p);
                    hub.changed(p);
                } else failed++;
            }
            for (var processor : blueprint.postProcessors())
                processor.process(level, origin, transform, Set.copyOf(touched), context);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Bulk placement scope", ex);
        }
        return new Result(placed, skipped, failed, touched);
    }
}
