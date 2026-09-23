package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.content.block.GeodeOreBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 晶簇矿的物品形态（《方块物品实体完整清单》§6.2.1.3 / §6.2.2.7）。
 *
 * <p>变体不再是 meta，但仍需在"挖下来再放回去"的往返中保持：星辉矿（ASTRAL）掉落的物品带
 * `Variant` 标签，放置时写回对应变体；无标签时按 `GEODE` 处理。这样 `geode_ore[ASTRAL]`
 * "掉落自身"的语义在 1.20.1 的无 meta 世界里依然成立。
 */
public final class GeodeOreItem extends BlockItem {

    /** 变体标签键。 */
    public static final String VARIANT = "variant";

    public GeodeOreItem(Block block) {
        super(block, new Item.Properties());
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        var state = super.getPlacementState(context);
        if (state == null) return null;
        return state.setValue(GeodeOreBlock.VARIANT, variantOf(context));
    }

    /** 读取手持物品的变体；缺省为 {@link GeodeOreBlock.Variant#GEODE}。 */
    public static GeodeOreBlock.Variant variantOf(BlockPlaceContext context) {
        var stack = context.getItemInHand();
        if (stack.hasTag()
                && stack.getTag()
                        .getString(VARIANT)
                        .equalsIgnoreCase(GeodeOreBlock.Variant.ASTRAL.getSerializedName()))
            return GeodeOreBlock.Variant.ASTRAL;
        return GeodeOreBlock.Variant.GEODE;
    }

    /** 物品 NBT 中的变体名；供掉落表与测试共用。 */
    public static String tagFor(GeodeOreBlock.Variant variant) {
        return variant.getSerializedName();
    }
}
