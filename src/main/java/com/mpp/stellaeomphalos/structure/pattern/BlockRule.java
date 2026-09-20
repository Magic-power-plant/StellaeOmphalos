package com.mpp.stellaeomphalos.structure.pattern;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Domain capability; predicates are immutable and never read or load a world. */
public interface BlockRule {
    boolean matches(BlockState state);

    default BlockState example() {
        return Blocks.AIR.defaultBlockState();
    }

    default BlockRule transform(PlacementTransform t) {
        return this;
    }

    default boolean isStrict() {
        return true;
    }

    static BlockRule state(BlockState state) {
        return new State(state);
    }

    static BlockRule tag(TagKey<Block> tag) {
        return new Tagged(tag);
    }

    static BlockRule air() {
        return new Air();
    }

    record State(BlockState value) implements BlockRule {
        public boolean matches(BlockState state) {
            return value.equals(state);
        }

        public BlockState example() {
            return value;
        }

        public BlockRule transform(PlacementTransform t) {
            return new State(t.apply(value));
        }
    }

    record Tagged(TagKey<Block> value) implements BlockRule {
        public boolean matches(BlockState state) {
            return state.is(value);
        }

        public BlockState example() {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getTag(value)
                    .flatMap(s -> s.stream().findFirst())
                    .map(h -> h.value().defaultBlockState())
                    .orElse(Blocks.AIR.defaultBlockState());
        }
    }

    record Air() implements BlockRule {
        public boolean matches(BlockState state) {
            return state.getFluidState().isEmpty() && (state.isAir() || state.canBeReplaced());
        }

        public boolean isStrict() {
            return false;
        }
    }

    record Solid() implements BlockRule {
        public boolean matches(BlockState state) {
            return !state.isAir()
                    && !state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
        }
    }

    record Composite(Mode mode, List<BlockRule> children) implements BlockRule {
        public enum Mode {
            OR,
            AND,
            NOT
        }

        public Composite {
            children = List.copyOf(children);
            if (children.isEmpty() || mode == Mode.NOT && children.size() != 1)
                throw new IllegalArgumentException("Invalid composite rule");
        }

        public boolean matches(BlockState state) {
            return switch (mode) {
                case OR -> children.stream().anyMatch(r -> r.matches(state));
                case AND -> children.stream().allMatch(r -> r.matches(state));
                case NOT -> !children.get(0).matches(state);
            };
        }

        public BlockState example() {
            return children.get(0).example();
        }

        public BlockRule transform(PlacementTransform t) {
            return new Composite(mode, children.stream().map(r -> r.transform(t)).toList());
        }
    }
}
