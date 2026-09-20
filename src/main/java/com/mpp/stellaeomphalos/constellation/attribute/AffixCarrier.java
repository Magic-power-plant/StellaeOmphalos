package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** 宝晶词条载体：物品与方块双形态抽象（1.20.1 用 ItemStack + BlockItem 判定方块形态）。 */
public interface AffixCarrier {

    Optional<CrystalAttunement> attunement(ItemStack stack);

    /** 无属性则补随机属性（三角分布），返回携带属性的堆叠。 */
    ItemStack ensureAttunement(ItemStack stack, RandomSource random);

    List<GemAffixModifier> affixes(ItemStack stack);

    void writeAffixes(ItemStack stack, List<GemAffixModifier> affixes);

    default boolean isBlockForm(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }
}
