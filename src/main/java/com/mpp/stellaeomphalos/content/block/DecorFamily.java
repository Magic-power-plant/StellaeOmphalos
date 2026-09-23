package com.mpp.stellaeomphalos.content.block;

import java.util.List;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * 装饰建材族（《方块物品实体完整清单》§6.2.1.1 / 决策 D-1）。
 *
 * <p>1.20.1 禁止 meta，故三个族各自只注册**一个** Block，变体由 {@code EnumProperty} 表达；每个可获得变体配一个独立
 * {@code BlockItem}。柱体变体额外使用 {@code top}/{@code bottom} 两个布尔属性实现自动连接（§6.3.2）。
 */
public enum DecorFamily implements StringRepresentable {
    MARBLE(
            "marble",
            List.of(
                    DecorVariant.RAW,
                    DecorVariant.BRICKS,
                    DecorVariant.PILLAR,
                    DecorVariant.ARCH,
                    DecorVariant.CHISELED,
                    DecorVariant.ENGRAVED,
                    DecorVariant.RUNED)),
    BLACK_MARBLE(
            "black_marble",
            List.of(
                    DecorVariant.RAW,
                    DecorVariant.BRICKS,
                    DecorVariant.PILLAR,
                    DecorVariant.ARCH,
                    DecorVariant.CHISELED,
                    DecorVariant.ENGRAVED,
                    DecorVariant.RUNED)),
    INFUSED_WOOD(
            "infused_wood",
            List.of(
                    DecorVariant.RAW,
                    DecorVariant.PLANKS,
                    DecorVariant.PILLAR,
                    DecorVariant.ARCH,
                    DecorVariant.ENGRAVED,
                    DecorVariant.ENRICHED,
                    DecorVariant.INFUSED));

    private final String id;
    private final List<DecorVariant> variants;

    DecorFamily(String id, List<DecorVariant> variants) {
        this.id = id;
        this.variants = variants;
    }

    public String id() {
        return id;
    }

    public List<DecorVariant> variants() {
        return variants;
    }

    public boolean supports(DecorVariant variant) {
        return variants.contains(variant);
    }

    /** 变体的物品 id：原石变体用族 id，其余追加变体后缀。 */
    public String itemId(DecorVariant variant) {
        return variant == DecorVariant.RAW ? id : id + "_" + variant.getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String translationKey() {
        return "block.stellaeomphalos." + id;
    }

    /** 变体形态（《方块物品实体完整清单》§6.2.1.1）：7 个可获得变体。 */
    public enum DecorVariant implements StringRepresentable {
        RAW,
        BRICKS,
        PILLAR,
        ARCH,
        CHISELED,
        ENGRAVED,
        RUNED,
        PLANKS,
        ENRICHED,
        INFUSED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
