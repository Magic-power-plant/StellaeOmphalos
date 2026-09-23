package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.block.DecorFamily.DecorVariant;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** 大理石砖台阶（《方块物品实体完整清单》§6.2.1.1 `marble_slab`）。 */
public class DecorSlabBlock extends SlabBlock {
    private final boolean fullByDefault;

    public DecorSlabBlock(Properties properties) {
        this(properties, false);
    }

    private DecorSlabBlock(Properties properties, boolean fullByDefault) {
        super(properties);
        this.fullByDefault = fullByDefault;
    }

    /**
     * 双半砖完整形态（`marble_double_slab`）。1.20.1 的合并由 {@link SlabBlock} 自身处理，本类仅保留注册条目，
     * 使注册总账与 §6.2.1.1 一致，且不产生创造页条目与掉落。
     */
    public static DecorSlabBlock doubleSlab(Properties properties) {
        var block = new DecorSlabBlock(properties, true);
        block.registerDefaultState(block.defaultBlockState().setValue(TYPE, SlabType.DOUBLE));
        return block;
    }

    public boolean isFullByDefault() {
        return fullByDefault;
    }
}
