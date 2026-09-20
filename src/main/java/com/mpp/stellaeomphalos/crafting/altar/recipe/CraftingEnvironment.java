package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.mpp.stellaeomphalos.lumen.capability.LumenCapability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;

/**
 * Part 4 may replace the matcher. Default frames use tagged two-block pillars and never load
 * chunks.
 */
public final class CraftingEnvironment {
    @FunctionalInterface
    public interface FrameMatcher {
        boolean matches(Level level, BlockPos pos, AsterismTier tier);
    }

    private static FrameMatcher matcher = CraftingEnvironment::defaultFrame;
    private static final TagKey<net.minecraft.world.level.block.Block> FRAME =
            TagKey.create(Registries.BLOCK, new ResourceLocation("stellaeomphalos", "altar_frame"));

    private CraftingEnvironment() {}

    public static void registerFrameMatcher(FrameMatcher next) {
        matcher = java.util.Objects.requireNonNull(next);
    }

    public static boolean structure(Level level, BlockPos pos, AsterismTier recipeTier) {
        return !recipeTier.requiresStructure() || matcher.matches(level, pos, recipeTier);
    }

    private static boolean defaultFrame(Level level, BlockPos origin, AsterismTier tier) {
        int radius = tier == AsterismTier.RESONANCE ? 2 : 3;
        int[][] columns =
                tier == AsterismTier.RESONANCE
                        ? new int[][] {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}
                        : new int[][] {
                            {-3, -3}, {-3, 3}, {3, -3}, {3, 3}, {0, -3}, {0, 3}, {-3, 0}, {3, 0}
                        };
        for (var column : columns)
            for (int y = -1; y <= 0; y++) {
                var p = origin.offset(column[0], y, column[1]);
                if (!level.hasChunkAt(p) || !level.getBlockState(p).is(FRAME)) return false;
            }
        return true;
    }

    public static long illumination(Level level, BlockPos pos, int distance) {
        long maximum = 0;
        // Inspect bounded loaded block-entity positions, not every block in a cubic scan.
        int minX = (pos.getX() - distance) >> 4,
                maxX = (pos.getX() + distance) >> 4,
                minZ = (pos.getZ() - distance) >> 4,
                maxZ = (pos.getZ() + distance) >> 4;
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                var chunk =
                        level.getChunkSource()
                                .getChunk(
                                        x,
                                        z,
                                        net.minecraft.world.level.chunk.ChunkStatus.FULL,
                                        false);
                if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk loaded)) continue;
                for (var be : loaded.getBlockEntities().values())
                    if (be.getBlockPos().distSqr(pos) <= distance * distance) {
                        var capability = be.getCapability(LumenCapability.LUMEN);
                        long stored = capability.map(h -> h.stored()).orElse(0L);
                        maximum = Math.max(maximum, stored);
                    }
            }
        return maximum;
    }
}
