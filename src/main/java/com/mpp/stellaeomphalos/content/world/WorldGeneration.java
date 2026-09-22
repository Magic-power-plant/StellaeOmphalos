package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.content.world.structure.*;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.*;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

import java.util.concurrent.*;

public final class WorldGeneration {
    public static final RegistrationGuard<GeodeOreFeature> GEODE =
            ModFeatures.ENTRIES.declare("geode_ore", GeodeOreFeature::new);
    public static final RegistrationGuard<SurfaceFeature>
            AQUAMARINE =
                    ModFeatures.ENTRIES.declare("aquamarine_sand", () -> new SurfaceFeature(true)),
            FLOWER = ModFeatures.ENTRIES.declare("glowbloom", () -> new SurfaceFeature(false));
    public static final RegistrationGuard<PlacementModifierType<InWaterPlacement>> IN_WATER =
            ModPlacementModifiers.ENTRIES.declare("in_water", () -> () -> InWaterPlacement.CODEC);
    public static final RegistrationGuard<StructureType<TempleStructure>> STRUCTURE =
            ModStructures.TYPES.declare("temple", () -> () -> TempleStructure.CODEC);
    public static final RegistrationGuard<StructurePieceType> PIECE =
            ModStructures.PIECES.declare("temple", () -> TempleStructurePiece::new);
    public static final RegistrationGuard<StructureProcessorType<PlaceholderProcessor>>
            PLACEHOLDER =
                    ModStructures.PROCESSORS.declare(
                            "placeholder", () -> () -> PlaceholderProcessor.CODEC);

    private record Generated(
            ResourceKey<Level> dimension, ResourceLocation target, BlockPos origin) {}

    private static final ConcurrentLinkedQueue<Generated> PENDING = new ConcurrentLinkedQueue<>();

    private WorldGeneration() {}

    public static void initialize() {}

    public static void record(
            ResourceKey<Level> dimension, ResourceLocation target, BlockPos origin) {
        if (PENDING.size() < 65536)
            PENDING.add(new Generated(dimension, target, origin.immutable()));
    }

    public static void drain(ServerLevel level) {
        int count = 0;
        for (var entry : PENDING) {
            if (count >= 256) break;
            if (entry.dimension().equals(level.dimension())) {
                PENDING.remove(entry);
                AstrolabeLedger.get(level).mark(entry.target(), entry.origin());
                count++;
            }
        }
    }

    public static void clear() {
        PENDING.clear();
    }
}
