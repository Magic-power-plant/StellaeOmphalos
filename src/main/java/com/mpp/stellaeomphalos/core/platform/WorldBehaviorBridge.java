package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Domain capabilities supplied by world content; lower layers never import content classes. */
public final class WorldBehaviorBridge {
    public interface Tables {
        boolean herdable(Animal animal);

        List<ItemStack> herdDrops(ServerLevel level, Animal animal);

        BlockState ore(RandomSource random, String dataset);

        net.minecraft.world.level.block.state.BlockState surfaceCover(
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome);

        boolean canAccelerate(ServerLevel level, BlockEntity entity);

        void failedAcceleration(ServerLevel level, BlockEntity entity, Exception failure);
    }

    private static Tables tables =
            new Tables() {
                public boolean herdable(Animal a) {
                    return false;
                }

                public List<ItemStack> herdDrops(ServerLevel l, Animal a) {
                    return List.of();
                }

                public BlockState ore(RandomSource r, String d) {
                    return null;
                }

                public BlockState surfaceCover(
                        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
                    return net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
                }

                public boolean canAccelerate(ServerLevel l, BlockEntity e) {
                    return false;
                }

                public void failedAcceleration(ServerLevel l, BlockEntity e, Exception x) {}
            };

    private WorldBehaviorBridge() {}

    public static void register(Tables value) {
        tables = Objects.requireNonNull(value);
    }

    public static Tables tables() {
        return tables;
    }
}
