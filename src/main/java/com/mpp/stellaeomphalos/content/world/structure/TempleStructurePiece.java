package com.mpp.stellaeomphalos.content.world.structure;

import com.mpp.stellaeomphalos.content.world.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/**
 * Vanilla template codec handles persistence and chunk clipping. Successful placement is queued.
 */
public final class TempleStructurePiece extends TemplateStructurePiece {
    public TempleStructurePiece(
            StructureTemplateManager manager,
            ResourceLocation template,
            BlockPos p,
            Rotation rotation) {
        super(
                WorldGeneration.PIECE.get(),
                0,
                manager,
                template,
                template.toString(),
                settings(rotation),
                p);
    }

    public TempleStructurePiece(StructurePieceSerializationContext context, CompoundTag n) {
        super(
                WorldGeneration.PIECE.get(),
                n,
                context.structureTemplateManager(),
                id -> settings(rotation(n)));
    }

    private static Rotation rotation(CompoundTag n) {
        try {
            return Rotation.valueOf(n.getString("Rotation"));
        } catch (IllegalArgumentException e) {
            return Rotation.NONE;
        }
    }

    private static StructurePlaceSettings settings(Rotation r) {
        return new StructurePlaceSettings()
                .setRotation(r)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(
                        new PlaceholderProcessor(
                                WorldContent.BLOCKS
                                        .get("marble_bricks")
                                        .get()
                                        .defaultBlockState()));
    }

    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context, CompoundTag n) {
        super.addAdditionalSaveData(context, n);
        n.putString("Rotation", getRotation().name());
    }

    protected void handleDataMarker(
            String marker,
            BlockPos p,
            ServerLevelAccessor level,
            RandomSource random,
            BoundingBox bounds) {
        if (!bounds.isInside(p)) return;
        if (marker.equals("treasure")) {
            level.setBlock(
                    p, WorldContent.BLOCKS.get("ore_regenerator").get().defaultBlockState(), 2);
        } else if (marker.equals("gate"))
            level.setBlock(p, WorldContent.BLOCKS.get("gate_core").get().defaultBlockState(), 2);
    }

    public void postProcess(
            WorldGenLevel level,
            StructureManager manager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunk,
            BlockPos pivot) {
        placeSettings.setBoundingBox(box);
        boolean success =
                template.placeInWorld(level, templatePosition, pivot, placeSettings, random, 2);
        if (success) {
            var id = new ResourceLocation(templateName);
            WorldGeneration.record(level.getLevel().dimension(), id, templatePosition);
            if (id.getPath().equals("desert_shrine"))
                for (var p :
                        BlockPos.betweenClosed(
                                box.minX(),
                                templatePosition.getY(),
                                box.minZ(),
                                box.maxX(),
                                Math.min(box.maxY(), templatePosition.getY() + 3),
                                box.maxZ())) {
                    if (!boundingBox.isInside(p) || !level.ensureCanWrite(p)) continue;
                    int dy = p.getY() - templatePosition.getY();
                    double probability = new double[] {1, 0.5, 0.4, 0.3}[dy];
                    if (random.nextDouble() < probability && level.getBlockState(p).isAir())
                        level.setBlock(
                                p,
                                com.mpp.stellaeomphalos.core.platform.WorldBehaviorBridge.tables()
                                        .surfaceCover(level.getBiome(p)),
                                2);
                }
        }
    }
}
