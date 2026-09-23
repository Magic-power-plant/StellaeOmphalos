package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.content.block.DecorFamily;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** 装饰变体的专用 BlockItem：把变体编码进物品，使方块状态在放置时确定（《方块物品实体完整清单》§6.3.1 D-1）。 */
public class DecorBlockItem extends BlockItem {
    private final DecorFamily family;
    private final DecorFamily.DecorVariant variant;

    public DecorBlockItem(
            Block block, DecorFamily family, DecorFamily.DecorVariant variant, Item.Properties properties) {
        super(block, properties);
        this.family = family;
        this.variant = variant;
    }

    public DecorFamily family() {
        return family;
    }

    public DecorFamily.DecorVariant variant() {
        return variant;
    }

    @Override
    public String getDescriptionId() {
        return "block.stellaeomphalos." + family.itemId(variant);
    }
}
