package com.mpp.stellaeomphalos.structure.match;

import com.mpp.stellaeomphalos.structure.pattern.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;

import java.util.*;

/** Sole matching authority. Chunk indexes avoid walking the full blueprint for local changes. */
public final class StructureAuthority {
    private final PatternBlueprint blueprint;
    private final BlockPos origin;
    private final Map<ChunkPos, List<BlockPos>> byChunk = new HashMap<>();
    private final Set<BlockPos> mismatches = new HashSet<>(), degradations = new HashSet<>();
    private final Set<ChunkPos> unresolved = new HashSet<>();
    private Boolean lastKnownFormed;
    private int required;

    public StructureAuthority(PatternBlueprint blueprint, BlockPos origin) {
        this.blueprint = blueprint;
        this.origin = origin.immutable();
        blueprint
                .blocks()
                .values()
                .forEach(
                        p -> {
                            byChunk.computeIfAbsent(
                                            new ChunkPos(origin.offset(p.relative())),
                                            k -> new ArrayList<>())
                                    .add(p.relative());
                            if (p.severity() == MismatchSeverity.REQUIRED) required++;
                        });
    }

    public Set<ChunkPos> chunks() {
        return Set.copyOf(byChunk.keySet());
    }

    public Set<BlockPos> mismatches() {
        return Set.copyOf(mismatches);
    }

    public Set<BlockPos> degradations() {
        return Set.copyOf(degradations);
    }

    public int unresolvedCount() {
        return unresolved.size();
    }

    public PatternBlueprint blueprint() {
        return blueprint;
    }

    public StructureState state() {
        return !mismatches.isEmpty()
                ? StructureState.BROKEN
                : !unresolved.isEmpty() || lastKnownFormed == null
                        ? StructureState.INDETERMINATE
                        : degradations.isEmpty() ? StructureState.FORMED : StructureState.DEGRADED;
    }

    public boolean formed() {
        return state().canProduce();
    }

    public double completeness() {
        return required == 0 ? 1 : 1 - (double) mismatches.size() / required;
    }

    public void initialize(Level level) {
        mismatches.clear();
        degradations.clear();
        unresolved.clear();
        for (var chunk : byChunk.keySet()) verifyChunk(level, chunk);
        lastKnownFormed = mismatches.isEmpty() && unresolved.isEmpty();
    }

    public void verifyChunk(Level level, ChunkPos chunk) {
        var positions = byChunk.get(chunk);
        if (positions == null) return;
        if (!level.hasChunk(chunk.x, chunk.z)) {
            unload(chunk);
            return;
        }
        unresolved.remove(chunk);
        positions.forEach(p -> verify(level, p));
        lastKnownFormed = mismatches.isEmpty() && unresolved.isEmpty();
    }

    public void unload(ChunkPos chunk) {
        if (!byChunk.containsKey(chunk)) return;
        unresolved.add(chunk);
        byChunk.get(chunk)
                .forEach(
                        p -> {
                            mismatches.remove(p);
                            degradations.remove(p);
                        });
    }

    private void verify(Level level, BlockPos relative) {
        var p = blueprint.blocks().get(relative);
        if (p == null) return;
        var bucket = p.severity() == MismatchSeverity.REQUIRED ? mismatches : degradations;
        if (p.rule().matches(level.getBlockState(origin.offset(relative)))) bucket.remove(relative);
        else bucket.add(relative);
    }

    public void changed(Level level, BlockPos absolute) {
        var relative = absolute.subtract(origin);
        if (!blueprint.blocks().containsKey(relative)) return;
        var chunk = new ChunkPos(absolute);
        if (!level.hasChunk(chunk.x, chunk.z)) {
            unload(chunk);
            return;
        }
        verify(level, relative);
    }

    public boolean onBatch(Level level, BlockChangeBatch batch) {
        if (batch.isEmpty()) return formed();
        batch.dirtyChunks().forEach(c -> verifyChunk(level, c));
        batch.changes().forEach(c -> changed(level, c.position()));
        return formed();
    }

    public boolean verifyLayer(Level level, int y) {
        for (var p : blueprint.blocks().keySet())
            if (p.getY() == y) changed(level, origin.offset(p));
        return mismatches.stream().noneMatch(p -> p.getY() == y) && unresolved.isEmpty();
    }

    public CompoundTag save() {
        var n = new CompoundTag();
        n.putString("Blueprint", blueprint.id().toString());
        n.putLongArray("Mismatch", mismatches.stream().mapToLong(BlockPos::asLong).toArray());
        n.putLongArray("Unresolved", unresolved.stream().mapToLong(ChunkPos::toLong).toArray());
        n.putByte("Formed", (byte) (lastKnownFormed == null ? -1 : lastKnownFormed ? 1 : 0));
        return n;
    }

    public void read(CompoundTag n) {
        mismatches.clear();
        for (long p : n.getLongArray("Mismatch")) {
            var pos = BlockPos.of(p);
            if (blueprint.blocks().containsKey(pos)) mismatches.add(pos);
        }
        unresolved.clear();
        for (long p : n.getLongArray("Unresolved")) {
            var c = new ChunkPos(p);
            if (byChunk.containsKey(c)) unresolved.add(c);
        }
        lastKnownFormed = n.getByte("Formed") == -1 ? null : n.getByte("Formed") == 1;
    }
}
