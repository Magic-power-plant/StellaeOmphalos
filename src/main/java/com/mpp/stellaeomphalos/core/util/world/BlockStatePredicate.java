package com.mpp.stellaeomphalos.core.util.world;

import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockStatePredicate {
    private BlockStatePredicate() {}
    public static Predicate<BlockState> block(Block block) { return state -> state.is(block); }
    public static Predicate<BlockState> exact(BlockState expected) { return state -> state.equals(expected); }
    public static <T extends Comparable<T>> Predicate<BlockState> property(Property<T> property, T expected) {
        return state -> state.hasProperty(property) && state.getValue(property).equals(expected);
    }
}
