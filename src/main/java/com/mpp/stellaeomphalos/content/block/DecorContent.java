package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.block.DecorFamily.DecorVariant;
import com.mpp.stellaeomphalos.content.item.DecorBlockItem;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModBlocks;
import com.mpp.stellaeomphalos.core.registry.ModItems;
import com.mpp.stellaeomphalos.data.loader.FoundationDataProvider;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 装饰变体族注册（Part-6 §6.2.1.1）。
 *
 * <p>注册 id：`marble` / `black_marble` / `infused_wood` 三个族 Block，每个族 7 个可获得变体物品，
 * 外加 `marble_slab` / `marble_double_slab` / `marble_stairs`。柱体（PILLAR）变体由
 * {@link DecorFamilyBlock} 的 `top`/`bottom` 属性自动连接。
 */
public final class DecorContent {

    public static final Map<DecorFamily, RegistrationGuard<DecorFamilyBlock>> FAMILY_BLOCKS =
            new LinkedHashMap<>();
    public static final Map<String, RegistrationGuard<? extends Item>> ITEM_BY_ID =
            new LinkedHashMap<>();

    public static final RegistrationGuard<? extends Block> MARBLE_SLAB;
    public static final RegistrationGuard<? extends Block> MARBLE_DOUBLE_SLAB;
    public static final RegistrationGuard<? extends Block> MARBLE_STAIRS;

    static {
        for (var family : DecorFamily.values()) {
            var guard =
                    ModBlocks.ENTRIES.declare(
                            family.id(),
                            () ->
                                    new DecorFamilyBlock(
                                            family,
                                            BlockBehaviour.Properties.of()
                                                    .mapColor(mapColorOf(family))
                                                    .strength(2.0F, 6.0F)
                                                    .sound(soundOf(family))
                                                    .requiresCorrectToolForDrops()));
            FAMILY_BLOCKS.put(family, guard);
            for (var variant : family.variants()) {
                String itemId = family.itemId(variant);
                ITEM_BY_ID.put(
                        itemId,
                        ModItems.ENTRIES.declare(
                                itemId,
                                () ->
                                        new DecorBlockItem(
                                                guard.get(),
                                                family,
                                                variant,
                                                new Item.Properties())));
            }
            lang(family.translationKey(), englishFamily(family), chineseFamily(family));
            for (var variant : family.variants())
                lang(
                        "block.stellaeomphalos." + family.itemId(variant),
                        englishVariant(family, variant),
                        chineseVariant(family, variant));
        }

        MARBLE_SLAB =
                ModBlocks.ENTRIES.declare(
                        "marble_slab",
                        () ->
                                new DecorSlabBlock(
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.QUARTZ)
                                                .strength(2.0F, 6.0F)
                                                .sound(SoundType.STONE)
                                                .requiresCorrectToolForDrops()));
        MARBLE_DOUBLE_SLAB =
                ModBlocks.ENTRIES.declare(
                        "marble_double_slab",
                        () ->
                                DecorSlabBlock.doubleSlab(
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.QUARTZ)
                                                .strength(2.0F, 6.0F)
                                                .sound(SoundType.STONE)
                                                .requiresCorrectToolForDrops()
                                                .noLootTable()));
        MARBLE_STAIRS =
                ModBlocks.ENTRIES.declare(
                        "marble_stairs",
                        () ->
                                new StairBlock(
                                        () ->
                                                FAMILY_BLOCKS
                                                        .get(DecorFamily.MARBLE)
                                                        .get()
                                                        .defaultBlockState()
                                                        .setValue(
                                                                DecorFamilyBlock.VARIANT,
                                                                DecorVariant.BRICKS),
                                        BlockBehaviour.Properties.of()
                                                .mapColor(MapColor.QUARTZ)
                                                .strength(2.0F, 6.0F)
                                                .sound(SoundType.STONE)
                                                .requiresCorrectToolForDrops()));
        ITEM_BY_ID.put(
                "marble_slab",
                ModItems.ENTRIES.declare(
                        "marble_slab",
                        () -> new BlockItem(MARBLE_SLAB.get(), new Item.Properties())));
        ITEM_BY_ID.put(
                "marble_stairs",
                ModItems.ENTRIES.declare(
                        "marble_stairs",
                        () -> new BlockItem(MARBLE_STAIRS.get(), new Item.Properties())));
        lang("block.stellaeomphalos.marble_slab", "Marble Bricks Slab", "大理石砖台阶");
        lang("block.stellaeomphalos.marble_double_slab", "Marble Bricks", "大理石砖");
        lang("block.stellaeomphalos.marble_stairs", "Marble Bricks Stairs", "大理石砖楼梯");
    }

    private DecorContent() {}

    public static void initialize() {}

    public static RegistrationGuard<DecorFamilyBlock> family(DecorFamily family) {
        return FAMILY_BLOCKS.get(family);
    }

    private static MapColor mapColorOf(DecorFamily family) {
        return switch (family) {
            case MARBLE -> MapColor.QUARTZ;
            case BLACK_MARBLE -> MapColor.COLOR_BLACK;
            case INFUSED_WOOD -> MapColor.WOOD;
        };
    }

    private static SoundType soundOf(DecorFamily family) {
        return family == DecorFamily.INFUSED_WOOD ? SoundType.WOOD : SoundType.STONE;
    }

    private static String englishFamily(DecorFamily family) {
        return switch (family) {
            case MARBLE -> "Marble";
            case BLACK_MARBLE -> "Black Marble";
            case INFUSED_WOOD -> "Infused Wood";
        };
    }

    private static String chineseFamily(DecorFamily family) {
        return switch (family) {
            case MARBLE -> "大理石";
            case BLACK_MARBLE -> "黑大理石";
            case INFUSED_WOOD -> "注魔木";
        };
    }

    private static String englishVariant(DecorFamily family, DecorVariant variant) {
        String base = englishFamily(family);
        return switch (variant) {
            case RAW -> base;
            case BRICKS -> base + " Bricks";
            case PILLAR -> base + " Pillar";
            case ARCH -> base + " Arch";
            case CHISELED -> base + " Chiseled";
            case ENGRAVED -> base + " Engraved";
            case RUNED -> base + " Runed";
            case PLANKS -> base + " Planks";
            case ENRICHED -> base + " Enriched";
            case INFUSED -> base + " Infused";
        };
    }

    private static String chineseVariant(DecorFamily family, DecorVariant variant) {
        String base = chineseFamily(family);
        return switch (variant) {
            case RAW -> base;
            case BRICKS -> base + "砖";
            case PILLAR -> base + "柱";
            case ARCH -> base + "拱块";
            case CHISELED -> base + "雕纹";
            case ENGRAVED -> base + "镌纹";
            case RUNED -> base + "符文";
            case PLANKS -> base + "木板";
            case ENRICHED -> base + "富集";
            case INFUSED -> base + "注魔";
        };
    }

    private static void lang(String key, String english, String chinese) {
        if (key.startsWith("block.stellaeomphalos."))
            FoundationDataProvider.language(key, english, chinese);
        else FoundationDataProvider.language("block." + Omphalos.MODID + "." + key, english, chinese);
    }
}
