package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Lower-layer capability lets constellation effects request a crafting-owned melt operation. */
public final class WorldCraftingBridge {
    public interface MeltOperation {
        int duration();

        boolean apply(ServerLevel level, BlockPos pos, BlockState expected);
    }

    @FunctionalInterface
    public interface Resolver {
        Optional<MeltOperation> resolve(ServerLevel level, BlockState state);
    }

    private static Resolver resolver = (level, state) -> Optional.empty();

    private WorldCraftingBridge() {}

    public static void register(Resolver value) {
        resolver = Objects.requireNonNull(value);
    }

    public static Optional<MeltOperation> melting(ServerLevel level, BlockState state) {
        return resolver.resolve(level, state);
    }
}
