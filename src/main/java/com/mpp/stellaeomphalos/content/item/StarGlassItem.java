package com.mpp.stellaeomphalos.content.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 星图玻璃（Part-6 §6.2.2.5 / §6.4.4）。
 *
 * <p>把已雕刻星图存在专用子标签 `Chart` 下（星象 id 列表 + 种子）；`hasFoil` 由"是否已雕刻"决定，
 * 等价原模组把"已激活"复用为附魔光效的做法。耐久损耗走 30%/级的自定义规则（配置键
 * `gameplay.starGlassUnbreakingChance` 的语义），**不**复用原版耐久附魔公式。
 */
public final class StarGlassItem extends Item {

    /** 星图子标签键。 */
    public static final String CHART = "Chart";

    /** 星图内星象列表键。 */
    public static final String CHART_SIGNS = "Signs";

    /** 星图种子键。 */
    public static final String CHART_SEED = "Seed";

    /** 基线耐久（§6.4.5 给定 100）。 */
    public static final int BASE_DURABILITY = 100;

    /** 每级自定义"耐久保留"概率。 */
    public static final float KEEP_CHANCE_PER_LEVEL = 0.3F;

    /** 允许的星象数量上限（§6.5 的雕刻包硬截断）。 */
    public static final int MAX_SIGNS = 3;

    public StarGlassItem() {
        super(new Item.Properties().stacksTo(1).durability(BASE_DURABILITY));
    }

    /** 已雕刻的星象列表；未雕刻时返回空表。 */
    public static List<ResourceLocation> chart(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(CHART, Tag.TAG_COMPOUND)) return List.of();
        var chart = tag.getCompound(CHART);
        var list = chart.getList(CHART_SIGNS, Tag.TAG_STRING);
        var result = new ArrayList<ResourceLocation>(list.size());
        for (int i = 0; i < list.size(); i++) {
            var id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) result.add(id);
        }
        return List.copyOf(result);
    }

    /** 星图种子；未雕刻时为 0。 */
    public static long seed(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(CHART, Tag.TAG_COMPOUND)) return 0L;
        return tag.getCompound(CHART).getLong(CHART_SEED);
    }

    /** 写入星图；超过 {@link #MAX_SIGNS} 的输入被硬截断。 */
    public static void engrave(ItemStack stack, List<ResourceLocation> signs, long seed) {
        var chart = new CompoundTag();
        var list = new ListTag();
        for (int i = 0; i < Math.min(MAX_SIGNS, signs.size()); i++)
            list.add(net.minecraft.nbt.StringTag.valueOf(signs.get(i).toString()));
        chart.put(CHART_SIGNS, list);
        chart.putLong(CHART_SEED, seed);
        stack.getOrCreateTag().put(CHART, chart);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !chart(stack).isEmpty();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 8;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        // §6.4.4：仅允许耐久类附魔（tag `#stellaeomphalos:star_glass_allowed` 的语义），
        // 用附魔类别判定，避免在不必要处引用数据包 tag。
        return enchantment.category == net.minecraft.world.item.enchantment.EnchantmentCategory.BREAKABLE
                || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return false;
    }

    /**
     * §6.4.4 的耐久损耗规则：每级以 {@value #KEEP_CHANCE_PER_LEVEL} 的概率**取消**本次损失。
     *
     * @return 是否应当跳过本次伤害
     */
    public static boolean keepsDurability(int unbreakingLevel, net.minecraft.util.RandomSource random) {
        if (unbreakingLevel <= 0) return false;
        for (int i = 0; i < unbreakingLevel; i++)
            if (random.nextFloat() < KEEP_CHANCE_PER_LEVEL) return true;
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag) {
        var signs = chart(stack);
        if (signs.isEmpty()) {
            tooltip.add(
                    Component.translatable("item.stellaeomphalos.star_glass.blank")
                            .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(
                Component.translatable("item.stellaeomphalos.star_glass.engraved", signs.size())
                        .withStyle(ChatFormatting.AQUA));
        for (var sign : signs)
            tooltip.add(
                    Component.literal(" - ")
                            .append(
                                    Component.translatable(
                                            "sign." + sign.getNamespace() + "." + sign.getPath()))
                            .withStyle(ChatFormatting.DARK_AQUA));
    }
}
