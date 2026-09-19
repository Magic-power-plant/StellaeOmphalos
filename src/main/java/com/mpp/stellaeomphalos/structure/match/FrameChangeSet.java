package com.mpp.stellaeomphalos.structure.match;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Coalesces changes by relative position while preserving the original old state. */
public final class FrameChangeSet {
    public record Change(BlockPos position, BlockState before, BlockState after) {
        public static final Codec<Change> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("Position").forGetter(Change::position),
                BlockState.CODEC.fieldOf("Before").forGetter(Change::before),
                BlockState.CODEC.fieldOf("After").forGetter(Change::after)).apply(instance, Change::new));
        public Change { position = position.immutable(); }
    }
    public static final Codec<FrameChangeSet> CODEC = Change.CODEC.listOf().xmap(FrameChangeSet::from, FrameChangeSet::snapshot);
    private final Map<BlockPos, Change> changes = new LinkedHashMap<>();
    public void add(BlockPos relative, BlockState before, BlockState after) {
        var first = changes.get(relative);
        changes.put(relative.immutable(), new Change(relative, first == null ? before : first.before(), after));
    }
    public List<Change> snapshot() { return List.copyOf(changes.values()); }
    public void clear() { changes.clear(); }
    private static FrameChangeSet from(List<Change> entries) {
        var result = new FrameChangeSet(); entries.forEach(change -> result.add(change.position(), change.before(), change.after())); return result;
    }
}
