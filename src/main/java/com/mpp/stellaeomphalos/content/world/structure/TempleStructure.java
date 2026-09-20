package com.mpp.stellaeomphalos.content.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.content.world.WorldGeneration;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;

import java.util.Optional;

public final class TempleStructure extends Structure {
    public static final Codec<TempleStructure> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            settingsCodec(i),
                                            ResourceLocation.CODEC
                                                    .fieldOf("template")
                                                    .forGetter(s -> s.template),
                                            Codec.intRange(0, 32)
                                                    .optionalFieldOf("depth", 0)
                                                    .forGetter(s -> s.depth))
                                    .apply(i, TempleStructure::new));
    private final ResourceLocation template;
    private final int depth;

    public TempleStructure(StructureSettings settings, ResourceLocation template, int depth) {
        super(settings);
        this.template = template;
        this.depth = depth;
    }

    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!OmphalosConfig.COMMON.flag("worldgen.enabled")) return Optional.empty();
        var chunk = context.chunkPos();
        int x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
        int y =
                context.chunkGenerator()
                        .getBaseHeight(
                                x + 7,
                                z + 7,
                                Heightmap.Types.WORLD_SURFACE_WG,
                                context.heightAccessor(),
                                context.randomState());
        if (y - depth < context.heightAccessor().getMinBuildHeight() + 8
                || y < context.chunkGenerator().getSeaLevel() - 8) return Optional.empty();
        var origin = new BlockPos(x, y - depth - 1, z);
        return Optional.of(
                new GenerationStub(
                        origin,
                        b ->
                                b.addPiece(
                                        new TempleStructurePiece(
                                                context.structureTemplateManager(),
                                                template,
                                                origin,
                                                Rotation.getRandom(context.random())))));
    }

    public ResourceLocation templateId() {
        return template;
    }

    public int depth() {
        return depth;
    }

    public StructureType<?> type() {
        return WorldGeneration.STRUCTURE.get();
    }
}
