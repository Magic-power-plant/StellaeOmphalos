package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Ordered writes, immutable snapshots and an explicit terminal lifecycle. */
public final class PatternBlueprintBuilder {
    private final ResourceLocation id;
    private final LinkedHashMap<BlockPos, BlockPlacement> blocks = new LinkedHashMap<>();
    private final Set<BlockPos> unique = new LinkedHashSet<>();
    private final List<PlacementProcessor> processors = new ArrayList<>();
    private boolean frozen, mirrorable = true, noPaste;
    private int overwritten;

    private PatternBlueprintBuilder(ResourceLocation id) {
        this.id = id;
    }

    public static PatternBlueprintBuilder named(ResourceLocation id) {
        return new PatternBlueprintBuilder(id);
    }

    private void writable() {
        if (frozen) throw new IllegalStateException("Blueprint is frozen");
    }

    public PatternBlueprintBuilder mirrorable(boolean b) {
        writable();
        mirrorable = b;
        return this;
    }

    public PatternBlueprintBuilder put(BlockPlacement p) {
        writable();
        if (blocks.size() >= 65536 && !blocks.containsKey(p.relative()))
            throw new IllegalArgumentException("Blueprint cell limit");
        if (Math.abs(p.relative().getX()) > 128
                || Math.abs(p.relative().getY()) > 128
                || Math.abs(p.relative().getZ()) > 128)
            throw new IllegalArgumentException("Blueprint bounds limit");
        var old = blocks.put(p.relative(), p);
        if (old != null && !old.rule().equals(p.rule())) overwritten++;
        return this;
    }

    public PatternBlueprintBuilder noPaste(boolean value) {
        writable();
        noPaste = value;
        return this;
    }

    public int overwrites() {
        return overwritten;
    }

    public PatternBlueprintBuilder block(BlockPos p, BlockRule r) {
        return put(new BlockPlacement(p, r, MismatchSeverity.REQUIRED));
    }

    public PatternBlueprintBuilder block(BlockPos p, BlockState s) {
        return block(p, BlockRule.state(s));
    }

    public PatternBlueprintBuilder block(BlockPos p, Block b) {
        return block(p, b.defaultBlockState());
    }

    public PatternBlueprintBuilder air(BlockPos p) {
        return block(p, BlockRule.air());
    }

    public PatternBlueprintBuilder tag(BlockPos p, TagKey<Block> tag) {
        return block(p, BlockRule.tag(tag));
    }

    public PatternBlueprintBuilder cube(BlockPos a, BlockPos b, BlockRule r) {
        if ((long) (Math.abs(a.getX() - b.getX()) + 1)
                        * (Math.abs(a.getY() - b.getY()) + 1)
                        * (Math.abs(a.getZ() - b.getZ()) + 1)
                > 65536) throw new IllegalArgumentException("Fill limit");
        BlockPos.betweenClosed(a, b).forEach(p -> block(p, r));
        return this;
    }

    public PatternBlueprintBuilder airCube(BlockPos a, BlockPos b) {
        return cube(a, b, BlockRule.air());
    }

    public PatternBlueprintBuilder ring(int y, int radius, BlockRule rule) {
        return ring(y, radius, rule, EnumSet.noneOf(Direction.class));
    }

    public PatternBlueprintBuilder ring(int y, int r, BlockRule rule, EnumSet<Direction> open) {
        if (r < 0 || r > 128) throw new IllegalArgumentException("Ring radius");
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
                if (Math.max(Math.abs(x), Math.abs(z)) == r
                        && !(x == r && open.contains(Direction.EAST)
                                || x == -r && open.contains(Direction.WEST)
                                || z == r && open.contains(Direction.SOUTH)
                                || z == -r && open.contains(Direction.NORTH)))
                    block(new BlockPos(x, y, z), rule);
        return this;
    }

    public PatternBlueprintBuilder placeholder(String name, BlockPos p, List<BlockRule> allowed) {
        if (name.isBlank()) throw new IllegalArgumentException("Empty placeholder");
        return block(p, new BlockRule.Composite(BlockRule.Composite.Mode.OR, allowed));
    }

    public PatternBlueprintBuilder placeholderArea(
            String name, BlockPos a, BlockPos b, List<BlockRule> allowed) {
        return cube(a, b, new BlockRule.Composite(BlockRule.Composite.Mode.OR, allowed));
    }

    public PatternBlueprintBuilder unique(BlockPos p) {
        writable();
        unique.add(p.immutable());
        return this;
    }

    public PatternBlueprintBuilder post(PlacementProcessor p) {
        writable();
        processors.add(p);
        return this;
    }

    public PatternBlueprintBuilder include(PatternBlueprint other, BlockPos offset) {
        return includeTransformed(other, offset, PlacementTransform.NONE);
    }

    public PatternBlueprintBuilder includeTransformed(
            PatternBlueprint other, BlockPos offset, PlacementTransform t) {
        writable();
        noPaste |= other.noPaste();
        var value = other.transformed(t);
        value.blocks()
                .values()
                .forEach(
                        p ->
                                put(
                                        new BlockPlacement(
                                                p.relative().offset(offset),
                                                p.rule(),
                                                p.severity())));
        value.uniqueSlots().forEach(p -> unique(p.offset(offset)));
        if (other instanceof BuildBlueprint build)
            for (var processor : build.postProcessors())
                processors.add(
                        (level, origin, transform, placed, ctx) ->
                                processor.process(
                                        level,
                                        origin.offset(transform.apply(offset)),
                                        transform.compose(t),
                                        placed,
                                        ctx));
        return this;
    }

    public BuildBlueprint build() {
        writable();
        frozen = true;
        if (blocks.isEmpty()) throw new IllegalArgumentException("Empty blueprint");
        if (!blocks.keySet().containsAll(unique))
            throw new IllegalArgumentException("Unique slot outside blueprint");
        return new Snapshot(
                id,
                Collections.unmodifiableMap(new LinkedHashMap<>(blocks)),
                Set.copyOf(unique),
                mirrorable,
                List.copyOf(processors),
                noPaste);
    }

    private record Snapshot(
            ResourceLocation id,
            Map<BlockPos, BlockPlacement> blocks,
            Set<BlockPos> uniqueSlots,
            boolean mirrorable,
            List<PlacementProcessor> postProcessors,
            boolean noPaste)
            implements BuildBlueprint {}
}
