package com.mpp.stellaeomphalos.content.item;

import net.minecraft.world.item.ItemStack;

/**
 * 高亮掉落物接口（《方块物品实体完整清单》§6.2.2.8）。
 *
 * <p>等价原模组的 `ItemHighlighted`：物品提供自己的高亮色，掉落时被替换为
 * {@code highlighted_item} 实体并同步该颜色。1.20.1 没有物品子类型系统，因此这是一个纯接口。
 */
public interface HighlightedItem {

    /** @return 掉落物光柱颜色（RGB） */
    int highlightColor(ItemStack stack);

    /** 默认高亮色：星辉蓝。 */
    int DEFAULT_HIGHLIGHT = 0x88BBFF;
}
