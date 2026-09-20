package com.mpp.stellaeomphalos.content.world.structure;

import com.mojang.serialization.Codec;
import com.mpp.stellaeomphalos.content.world.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public final class PlaceholderProcessor extends StructureProcessor {
    public static final Codec<PlaceholderProcessor> CODEC =
            BlockState.CODEC
                    .fieldOf("replacement")
                    .xmap(PlaceholderProcessor::new, p -> p.replacement)
                    .codec();
    private final BlockState replacement;

    public PlaceholderProcessor(BlockState replacement) {
        this.replacement = replacement;
    }

    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos origin,
            BlockPos pivot,
            StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo current,
            StructurePlaceSettings settings) {
        return current.state().is(WorldContent.BLOCKS.get("placeholder").get())
                ? new StructureTemplate.StructureBlockInfo(
                        current.pos(), replacement, current.nbt())
                : current;
    }

    protected StructureProcessorType<?> getType() {
        return WorldGeneration.PLACEHOLDER.get();
    }
}
