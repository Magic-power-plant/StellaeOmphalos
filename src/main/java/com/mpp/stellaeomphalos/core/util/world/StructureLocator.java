package com.mpp.stellaeomphalos.core.util.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class StructureLocator {
    private StructureLocator() {}
    /** Explicit world-generation query, unlike ordinary loaded-chunk-only access. */
    public static Optional<BlockPos> structure(ServerLevel level, TagKey<Structure> structures, BlockPos origin, int chunkRadius) {
        if (chunkRadius < 1 || chunkRadius > 128) throw new IllegalArgumentException("Invalid structure search radius");
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Structure query off server thread");
        return Optional.ofNullable(level.findNearestMapStructure(structures, origin, chunkRadius, false));
    }
    public static Optional<BlockPos> biome(ServerLevel level, TagKey<Biome> biomes, BlockPos origin, int radius) {
        if (radius < 1 || radius > 4096) throw new IllegalArgumentException("Invalid biome search radius");
        var result = level.findClosestBiome3d(holder -> holder.is(biomes), origin, radius, 32, 64);
        return Optional.ofNullable(result).map(pair -> pair.getFirst());
    }
}
