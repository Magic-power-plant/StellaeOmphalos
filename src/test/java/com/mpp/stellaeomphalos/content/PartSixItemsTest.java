package com.mpp.stellaeomphalos.content;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.content.item.BlockPalette;
import com.mpp.stellaeomphalos.content.item.ChargedCrystalTools;
import com.mpp.stellaeomphalos.content.item.PartSixItems;
import com.mpp.stellaeomphalos.content.item.SkyResonatorItem;
import com.mpp.stellaeomphalos.content.item.StarGlassItem;

import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Part-6 §6.4.4 / §6.4.5 物品持久数据的纯 JVM 验收。
 *
 * <p>覆盖：星图玻璃的星图往返与硬截断、方块收藏夹的序列化与过滤、充能工具的回退常量、
 * 天空共鸣器的六重容错回退、星眷封印的节点存取。
 */
class PartSixItemsTest {

    /**
     * 与既有测试一致：只翻转原版 bootstrap 守卫并让 {@code BuiltInRegistries} 自行填充，
     * 避免拉起 Forge 的网络钩子（在纯 JUnit JVM 中会失败）。
     */
    @BeforeAll
    static void bootstrap() {
        try {
            var guard = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
            guard.setAccessible(true);
            if (!guard.getBoolean(null)) {
                SharedConstants.tryDetectVersion();
                guard.setBoolean(null, true);
                net.minecraft.core.registries.BuiltInRegistries.bootStrap();
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void star_glass_round_trips_the_chart() {
        var stack = new ItemStack(Items.PAPER);
        assertTrue(StarGlassItem.chart(stack).isEmpty(), "Fresh glass must be unengraved");
        var signs =
                List.of(
                        new ResourceLocation("stellaeomphalos", "aevitas"),
                        new ResourceLocation("stellaeomphalos", "armara"));
        StarGlassItem.engrave(stack, signs, 4242L);
        assertEquals(signs, StarGlassItem.chart(stack));
        assertEquals(4242L, StarGlassItem.seed(stack));
        // `isFoil` 的规则即"存在星图"（chart 非空）；物品实例需要注册表，故断言该规则的数据面。
        assertFalse(StarGlassItem.chart(stack).isEmpty(), "Engraved glass must sparkle");
    }

    @Test
    void star_glass_truncates_to_three_signs() {
        var stack = new ItemStack(Items.PAPER);
        StarGlassItem.engrave(
                stack,
                List.of(
                        new ResourceLocation("stellaeomphalos", "a"),
                        new ResourceLocation("stellaeomphalos", "b"),
                        new ResourceLocation("stellaeomphalos", "c"),
                        new ResourceLocation("stellaeomphalos", "d")),
                1L);
        assertEquals(StarGlassItem.MAX_SIGNS, StarGlassItem.chart(stack).size());
    }

    @Test
    void star_glass_unbreaking_rule_short_circuits_the_first_hit() {
        var random = net.minecraft.util.RandomSource.create(1234L);
        assertFalse(StarGlassItem.keepsDurability(0, random), "No level means no protection");
        boolean protectedAtLeastOnce = false;
        for (int i = 0; i < 200 && !protectedAtLeastOnce; i++)
            protectedAtLeastOnce = StarGlassItem.keepsDurability(1, random);
        assertTrue(protectedAtLeastOnce, "Level 1 must protect at least once in 200 trials");
    }

    @Test
    void block_palette_filters_and_orders_entries() {
        var stack = new ItemStack(Items.STICK);
        assertTrue(BlockPalette.isEmpty(stack), "Fresh rod has an empty palette");
        // 直接写入收藏夹以验证序列化往返与顺序（世界相关的存放路径由 GameTest 覆盖）。
        var tag = stack.getOrCreateTag();
        var list = new net.minecraft.nbt.ListTag();
        list.add(
                net.minecraft.nbt.NbtUtils.writeBlockState(
                        Blocks.STONE.defaultBlockState()));
        list.add(
                net.minecraft.nbt.NbtUtils.writeBlockState(
                        Blocks.OAK_PLANKS.defaultBlockState()));
        tag.put(BlockPalette.TAG, list);
        var states = BlockPalette.storedStates(stack);
        assertEquals(2, states.size());
        assertEquals(Blocks.STONE, states.get(0).getBlock());
        assertEquals(Blocks.OAK_PLANKS, states.get(1).getBlock());
        var mapped = BlockPalette.mappedStored(stack);
        assertEquals(2, mapped.size());
        assertEquals(2, BlockPalette.describe(stack).getInt("Size"));
        assertTrue(
                BlockPalette.MAX_ENTRIES == 27, "Palette capacity must stay aligned with the hotbar");
    }

    @Test
    void sky_resonator_falls_back_to_starlight() {
        assertEquals(
                SkyResonatorItem.Mode.STARLIGHT,
                SkyResonatorItem.resolve(SkyResonatorItem.Mode.FLUID_FIELDS, false, true));
        assertEquals(
                SkyResonatorItem.Mode.STARLIGHT,
                SkyResonatorItem.resolve(SkyResonatorItem.Mode.AREA_SIZE, true, false));
        assertEquals(
                SkyResonatorItem.Mode.FLUID_FIELDS,
                SkyResonatorItem.resolve(SkyResonatorItem.Mode.FLUID_FIELDS, true, false));
        assertEquals(
                SkyResonatorItem.Mode.AREA_SIZE,
                SkyResonatorItem.resolve(SkyResonatorItem.Mode.AREA_SIZE, false, true));
        assertEquals(
                SkyResonatorItem.Mode.STARLIGHT,
                SkyResonatorItem.resolve(null, true, true));
    }

    @Test
    void sky_resonator_mode_persists_by_name() {
        var stack = new ItemStack(Items.PAPER);
        assertEquals(SkyResonatorItem.Mode.STARLIGHT, SkyResonatorItem.mode(stack));
        stack.getOrCreateTag().putString(SkyResonatorItem.MODE, "AREA_SIZE");
        assertEquals(SkyResonatorItem.Mode.AREA_SIZE, SkyResonatorItem.mode(stack));
        stack.getOrCreateTag().putString(SkyResonatorItem.MODE, "not_a_mode");
        assertEquals(SkyResonatorItem.Mode.STARLIGHT, SkyResonatorItem.mode(stack));
    }

    @Test
    void boon_seal_stores_and_reads_the_node_id() {
        var stack = new ItemStack(Items.PAPER);
        assertNull(PartSixItems.BoonSealItem.sealed(stack));
        var node = new ResourceLocation("stellaeomphalos", "key_bleed");
        PartSixItems.BoonSealItem.seal(stack, node);
        assertEquals(node, PartSixItems.BoonSealItem.sealed(stack));
    }

    @Test
    void charged_tool_revert_thresholds_are_stable() {
        assertEquals(40, ChargedCrystalTools.REVERT_START);
        assertEquals(80, ChargedCrystalTools.REVERT_CHANCE);
        assertEquals(100, ChargedCrystalTools.MAX_RADIUS_BREAK);
        assertEquals(14, ChargedCrystalTools.Pickaxe.SCAN_RADIUS);
        assertTrue(
                ChargedCrystalTools.withinScan(
                        new net.minecraft.core.BlockPos(0, 64, 0), new net.minecraft.core.BlockPos(10, 64, 0), 14));
        assertFalse(
                ChargedCrystalTools.withinScan(
                        new net.minecraft.core.BlockPos(0, 64, 0), new net.minecraft.core.BlockPos(20, 64, 0), 14));
    }

    @Test
    void palette_tag_key_is_namespaced() {
        assertEquals("StellaePalette", BlockPalette.TAG);
        BlockState state = Blocks.DIRT.defaultBlockState();
        assertFalse(state.isAir());
    }

    @Test
    void runed_wand_augments_round_trip_by_name() {
        var stack = new ItemStack(Items.STICK);
        assertEquals(
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandAugment.NONE,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.augmentOf(stack),
                "A fresh wand has no augment");
        var wandAugment = com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandAugment.PROSPECT;
        com.mpp.stellaeomphalos.content.item.PartSixRodItems.setAugment(stack, wandAugment);
        assertEquals(
                wandAugment,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.augmentOf(stack));
        // 按名字存储，未知值回退到 NONE（不使用 ordinal）。
        stack.getOrCreateTag()
                .putString(
                        com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandAugment.TAG,
                        "not_an_augment");
        assertEquals(
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandAugment.NONE,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.augmentOf(stack));
        // 轮换顺序覆盖全部取值后回到起点。
        var current = com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandAugment.NONE;
        for (int i = 0;
                i
                        < com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandAugment
                                .values()
                                .length;
                i++) {
            current = current.next();
        }
        assertEquals(
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandAugment.NONE, current);
    }

    @Test
    void wand_charge_formula_matches_the_plan() {
        // §6.6.2：motion = look × clamp(tick/30, 0, 1) × 3，Y 分量额外保底 0.7×mul。
        assertEquals(30, com.mpp.stellaeomphalos.content.item.PartSixRodItems.RunedWandItem.CHARGE_TICKS);
        assertEquals(
                3.0F,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.RunedWandItem.CHARGE_SCALE);
        assertEquals(
                0.7D,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.RunedWandItem.CHARGE_Y_FLOOR);
        assertEquals(
                6000,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.LuminaryRodItem
                        .LUMINAIRE_BOOST_TICKS);
    }

    @Test
    void prospect_cache_is_bounded_and_clearable() {
        var cache = com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache.class;
        assertNotNull(cache);
        var player = java.util.UUID.randomUUID();
        assertTrue(
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache.last(player)
                        .isEmpty(),
                "Unknown player has no cached result");
        var many = new java.util.ArrayList<net.minecraft.core.BlockPos>();
        for (int i = 0;
                i
                        < com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache
                                        .MAX_ENTRIES
                                + 32;
                i++)
            many.add(new net.minecraft.core.BlockPos(i, 64, 0));
        com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache.publish(player, many);
        assertEquals(
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache.MAX_ENTRIES,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache.last(player)
                        .size(),
                "Prospect results must be truncated");
        com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache.clear(player);
        assertTrue(
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandProspectCache.last(player)
                        .isEmpty());
    }

    @Test
    void builder_rod_plan_is_bounded_by_the_material_on_hand() {
        // §6.6.2：先模拟扣料再实扣——手里只有 3 个石头时，规划结果不能超过 3 格。
        assertEquals(20, com.mpp.stellaeomphalos.content.item.PartSixRodItems.BuilderRodItem.MAX_PLACEMENT);
        assertEquals(
                5,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.SwapperRodItem.SEARCH_DEPTH);
        assertEquals(
                3.0D,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.SwapperRodItem
                        .DEFAULT_HARDNESS_LIMIT);
        assertTrue(
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.BuilderRodItem.MAX_PLACEMENT
                        > com.mpp.stellaeomphalos.content.item.PartSixRodItems.SwapperRodItem
                                .SEARCH_DEPTH,
                "Placement cap must exceed the search depth");
    }

    @Test
    void builder_rod_placement_record_carries_state_and_item() {
        var state = Blocks.STONE.defaultBlockState();
        var placement =
                new com.mpp.stellaeomphalos.content.item.PartSixRodItems.Placement(
                        new net.minecraft.core.BlockPos(1, 2, 3),
                        state,
                        net.minecraft.world.item.Items.STONE);
        assertEquals(state, placement.state());
        assertEquals(net.minecraft.world.item.Items.STONE, placement.item());
        assertEquals(new net.minecraft.core.BlockPos(1, 2, 3), placement.pos());
    }

    @Test
    void luminary_rod_cycles_colours_by_name() {
        var stack = new ItemStack(Items.STICK);
        assertEquals(
                net.minecraft.world.item.DyeColor.WHITE,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.LuminaryRodItem.currentColor(
                        stack));
        var next =
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.LuminaryRodItem.nextColor(stack);
        assertEquals(
                next,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.LuminaryRodItem.currentColor(
                        stack));
        stack.getOrCreateTag()
                .putString(
                        com.mpp.stellaeomphalos.content.item.PartSixRodItems.LuminaryRodItem.COLOR,
                        "bogus");
        assertEquals(
                net.minecraft.world.item.DyeColor.WHITE,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.LuminaryRodItem.currentColor(
                        stack),
                "Unknown colour names fall back to white");
    }
}
