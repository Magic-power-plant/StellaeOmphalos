package com.mpp.stellaeomphalos.content;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.entity.catalog.LucentSparkEntity;
import com.mpp.stellaeomphalos.content.item.BlockPalette;
import com.mpp.stellaeomphalos.content.item.CatalogItems;
import com.mpp.stellaeomphalos.content.item.RodItems;
import com.mpp.stellaeomphalos.content.item.StarGlassItem;
import com.mpp.stellaeomphalos.content.world.WorldContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** 《方块物品实体完整清单》§6.2.2 物品注册总账与 §6.4.4 / §6.6.2 运行期验收。 */
@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class CatalogItemsGameTests {

    private static final List<String> ITEM_LEDGER =
            List.of(
                    "aquamarine",
                    "astral_ingot",
                    "star_dust",
                    "lens_blank",
                    "resonance_gem",
                    "parchment",
                    "geode",
                    "sky_crystal",
                    "resonant_geode",
                    "resonant_sky_crystal",
                    "warp_star",
                    "boon_gem_sky",
                    "boon_gem_day",
                    "boon_gem_night",
                    "boon_seal",
                    "geode_axe",
                    "geode_pickaxe",
                    "geode_shovel",
                    "geode_sword",
                    "charged_geode_axe",
                    "charged_geode_pickaxe",
                    "charged_geode_shovel",
                    "charged_geode_sword",
                    "runed_wand",
                    "builder_rod",
                    "swapper_rod",
                    "luminary_rod",
                    "grapnel_rod",
                    "astrolabe",
                    "spyglass_handheld",
                    "sky_resonator",
                    "resonance_linker",
                    "rosewood_bow",
                    "illumination_dust",
                    "nocturnal_dust",
                    "lore_shard",
                    "lore_capsule",
                    "lore_scroll",
                    "codex",
                    "sign_chart",
                    "star_glass",
                    "mantle",
                    "warded_amulet");

    @GameTest(template = "foundation_empty")
    public static void builder_rod_simulates_then_commits_the_material(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.STONE);
        var stonePos = helper.absolutePos(new BlockPos(4, 1, 2));

        var player =
                new net.minecraft.server.level.ServerPlayer(
                        level.getServer(),
                        level,
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.randomUUID(), "part6-rod"));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.getAbilities().instabuild = false;
        player.getInventory().setItem(0, new ItemStack(Items.STONE, 2));

        var rod = new ItemStack(CatalogItems.BUILDER_ROD.get());
        helper.assertTrue(BlockPalette.tryStore(rod, level, stonePos), "Stone must be storable");
        var states = BlockPalette.storedStates(rod);
        helper.assertTrue(states.size() == 1, "Palette must hold exactly one state");

        // 候选远超手里的料：规划必须被"模拟扣料"截断到 2 格。
        var base = anchor.above();
        var candidates =
                List.of(
                        base,
                        base.west(1),
                        base.east(1),
                        base.north(1),
                        base.south(1),
                        base.above());
        var plan =
                com.mpp.stellaeomphalos.content.item.RodItems.plan(
                        level,
                        player,
                        candidates,
                        states,
                        com.mpp.stellaeomphalos.content.item.RodItems.REPLACEABLE_ONLY,
                        com.mpp.stellaeomphalos.content.item.RodItems.BuilderRodItem
                                .MAX_PLACEMENT);
        helper.assertTrue(
                plan.size() == 2,
                "Planning must stop when the simulated material runs out, got " + plan.size());

        int placed =
                com.mpp.stellaeomphalos.content.item.RodItems.commit(level, player, plan);
        helper.assertTrue(placed == 2, "Commit must place every planned cell, placed " + placed);

        int stoneLeft = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(Items.STONE)) stoneLeft += stack.getCount();
        }
        helper.assertTrue(
                stoneLeft == 0,
                "Exactly the planned amount must be consumed, left " + stoneLeft);
        for (var placement : plan)
            helper.assertTrue(
                    level.getBlockState(placement.pos()).is(Blocks.STONE),
                    "Planned cell must hold the stored state at " + placement.pos());

        // 空手时不再规划（料为 0）。
        player.getInventory().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.item.RodItems.plan(
                                level,
                                player,
                                candidates,
                                states,
                                com.mpp.stellaeomphalos.content.item.RodItems
                                        .REPLACEABLE_ONLY,
                                20)
                        .isEmpty(),
                "No material means no plan");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void amulet_applies_without_an_explicit_context(GameTestHelper helper) {
        // §6.3.5 的最终形态：不需要调用方包夹上下文，桥按引用相等反查物品所有者。
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(2, 1, 2));
        var player =
                new net.minecraft.server.level.ServerPlayer(
                        level.getServer(),
                        level,
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.randomUUID(), "part6-amulet"));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);

        var sharpness =
                net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(
                        new ResourceLocation("minecraft", "sharpness"));
        helper.assertTrue(sharpness != null, "Vanilla sharpness must exist");
        var sign = new ResourceLocation("minecraft", "sharpness");

        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(sharpness, 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);

        var amulet = new ItemStack(CatalogItems.WARDED_AMULET.get());
        com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder.setModifiers(
                amulet,
                List.of(
                        new com.mpp.stellaeomphalos.content.item.amulet.AmuletModifier(
                                com.mpp.stellaeomphalos.content.item.amulet.AmuletModifier
                                        .ModifierKind.ADD_SPECIFIC,
                                java.util.Optional.of(sign),
                                2)));
        player.getInventory().setItem(9, amulet);
        // 玩家 tick 会做同样的事；测试里显式登记身份索引。
        com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge.track(player);

        com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge.clearContext();
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge.owns(
                        player, player.getMainHandItem()),
                "The holder must be resolvable by item identity");
        helper.assertTrue(
                net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                sharpness, player.getMainHandItem())
                        == 3,
                "The amulet must boost the holder's own item without an explicit context");
        // 无主物品不受影响（同一件物品没有任何玩家持有时不施加修正）。
        var foreign = new ItemStack(Items.DIAMOND_SWORD);
        foreign.enchant(sharpness, 1);
        helper.assertTrue(
                net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                sharpness, foreign)
                        == 1,
                "An item nobody holds must not receive the amulet bonus");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void drops_are_replaced_by_the_specialised_item_entities(GameTestHelper helper) {
        // §6.6.2：水晶 → 晶簇实体、星屑 → 星屑实体、充能工具 → 晶簇工具实体。
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.item.ItemEntityReplacement.replacementFor(
                                new ItemStack(CatalogItems.SKY_CRYSTAL.get()))
                        == com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities.GEODE_ENTITY
                                .get(),
                "A sky crystal drop must become a geode entity");
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.item.ItemEntityReplacement.replacementFor(
                                new ItemStack(CatalogItems.CHARGED_GEODE_PICKAXE.get()))
                        == com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities
                                .GEODE_TOOL_ENTITY
                                .get(),
                "A charged tool drop must become a geode tool entity");
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.item.ItemEntityReplacement.replacementFor(
                                new ItemStack(
                                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                                                new ResourceLocation(
                                                        Omphalos.MODID, "star_dust"))))
                        == com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities.STAR_DUST
                                .get(),
                "A star dust drop must become a star dust entity");
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.item.ItemEntityReplacement.replacementFor(
                                new ItemStack(Items.STONE))
                        == null,
                "Ordinary items must keep the vanilla item entity");

        // 真正丢一个水晶：加入世界的必须已经是专用实体（幂等、不递归）。
        var geodeItem = CatalogItems.SKY_CRYSTAL.get();
        var dropped = new net.minecraft.world.entity.item.ItemEntity(
                helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 2)).getX() + 0.5D,
                helper.absolutePos(new BlockPos(2, 2, 2)).getY(),
                helper.absolutePos(new BlockPos(2, 2, 2)).getZ() + 0.5D,
                new ItemStack(geodeItem));
        helper.getLevel().addFreshEntity(dropped);
        var replaced =
                helper.getLevel().getEntitiesOfClass(
                        com.mpp.stellaeomphalos.content.entity.catalog.GeodeEntity.class,
                        new net.minecraft.world.phys.AABB(
                                        helper.absolutePos(new BlockPos(2, 2, 2))).inflate(2.0D));
        helper.assertTrue(
                !replaced.isEmpty(),
                "The joined drop must have been swapped for the specialised entity");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void runed_wand_aevitas_paves_phase_barriers(GameTestHelper helper) {
        var player =
                new net.minecraft.server.level.ServerPlayer(
                        helper.getLevel().getServer(),
                        helper.getLevel(),
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.randomUUID(), "part6-wand"));
        var anchor = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        var stack = new ItemStack(CatalogItems.RUNED_WAND.get());
        RodItems.setAugment(stack, RodItems.WandAugment.AEVITAS);
        // 选中槽位 + AEVITAS → 铺路（§6.6.2）。
        stack.inventoryTick(helper.getLevel(), player, 0, true);
        var barrier = WorldContent.BLOCKS.get("phase_barrier").get();
        int placed = 0;
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                if (helper.getLevel().getBlockState(anchor.offset(x, -1, z)).is(barrier)) placed++;
        helper.assertTrue(placed >= 1, "AEVITAS must pave at least one barrier under the holder");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void runed_wand_vicio_release_applies_the_charge_impulse(GameTestHelper helper) {
        var player =
                new net.minecraft.server.level.ServerPlayer(
                        helper.getLevel().getServer(),
                        helper.getLevel(),
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.randomUUID(), "part6-leap"));
        var anchor = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        player.setXRot(0.0F);
        player.setYRot(0.0F);
        var stack = new ItemStack(CatalogItems.RUNED_WAND.get());
        RodItems.setAugment(stack, RodItems.WandAugment.VICIO);
        var item = (RodItems.RunedWandItem) stack.getItem();
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        // 满蓄力：motion = look × 1 × 3，Y 分量保底 0.7 × mul。
        item.releaseUsing(
                stack,
                helper.getLevel(),
                player,
                item.getUseDuration(stack) - RodItems.RunedWandItem.CHARGE_TICKS);
        var motion = player.getDeltaMovement();
        helper.assertTrue(motion.lengthSqr() > 0.0D, "A full charge must produce an impulse");
        helper.assertTrue(
                motion.y
                        >= RodItems.RunedWandItem.CHARGE_Y_FLOOR
                                        * RodItems.RunedWandItem.CHARGE_SCALE
                                - 1.0E-6D,
                "The Y component must respect the floor, got " + motion.y);
        helper.assertTrue(
                player.fallDistance == 0.0F, "The leap must clear accumulated fall distance");
        // 未蓄力：不产生冲量。
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        item.releaseUsing(stack, helper.getLevel(), player, item.getUseDuration(stack));
        helper.assertTrue(
                player.getDeltaMovement().lengthSqr() == 0.0D,
                "A zero-length charge must not move the holder");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void item_ledger_is_registered(GameTestHelper helper) {
        for (String id : ITEM_LEDGER)
            helper.assertTrue(
                    BuiltInRegistries.ITEM.containsKey(new ResourceLocation(Omphalos.MODID, id)),
                    "Missing Part-6 item registration: " + id);
        // §6.2.2.5：玫瑰木弓是遗留物品，**不进创造页**；这里只断言它确实已注册。
        helper.assertTrue(
                BuiltInRegistries.ITEM.containsKey(
                        new ResourceLocation(Omphalos.MODID, "rosewood_bow")),
                "Legacy bow must still exist");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void builder_rod_stores_block_states_with_three_filters(GameTestHelper helper) {
        var level = helper.getLevel();
        var rod = new ItemStack(CatalogItems.BUILDER_ROD.get());
        // GameTestHelper 的坐标是结构相对坐标，而方块读写走世界坐标，必须显式换算。
        var plain = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        helper.assertTrue(BlockPalette.tryStore(rod, level, plain), "Plain stone must be storable");
        helper.assertTrue(BlockPalette.storedStates(rod).size() == 1, "Palette must hold one entry");

        // 过滤 3：硬度 -1（不可破坏）不收。
        var bedrock = helper.absolutePos(new BlockPos(3, 1, 2));
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.BEDROCK);
        helper.assertTrue(!BlockPalette.tryStore(rod, level, bedrock), "Unbreakable blocks are refused");
        helper.assertTrue(BlockPalette.storedStates(rod).size() == 1, "Palette unchanged after refusal");

        // 过滤 1：有方块实体不收（箱子）。
        var chest = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.CHEST);
        helper.assertTrue(!BlockPalette.tryStore(rod, level, chest), "Block entities are refused");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void illumination_dust_places_the_ephemeral_light(GameTestHelper helper) {
        var level = helper.getLevel();
        var target = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        helper.assertTrue(LucentSparkEntity.canBeLit(level, target), "Empty cell must be lightable");
        helper.assertTrue(LucentSparkEntity.placeLightAt(level, target), "Light placement must succeed");
        helper.assertTrue(
                level.getBlockState(target)
                        .is(
                                net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(
                                        new ResourceLocation(Omphalos.MODID, "ephemeral_light"))),
                "Placed block must be ephemeral_light");
        // 第二次放置必须被拒绝（目标已不可替换）。
        helper.assertTrue(!LucentSparkEntity.placeLightAt(level, target), "Occupied cell is refused");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void star_glass_engraving_is_capped_and_persistent(GameTestHelper helper) {
        var stack = new ItemStack(CatalogItems.STAR_GLASS.get());
        helper.assertTrue(StarGlassItem.chart(stack).isEmpty(), "Fresh glass is unengraved");
        StarGlassItem.engrave(
                stack,
                List.of(
                        new ResourceLocation(Omphalos.MODID, "aevitas"),
                        new ResourceLocation(Omphalos.MODID, "armara"),
                        new ResourceLocation(Omphalos.MODID, "discidia"),
                        new ResourceLocation(Omphalos.MODID, "evorsio")),
                7L);
        helper.assertTrue(
                StarGlassItem.chart(stack).size() == StarGlassItem.MAX_SIGNS,
                "Engraving must be hard-truncated to three signs");
        helper.assertTrue(stack.hasFoil(), "Engraved glass must sparkle");
        helper.assertTrue(StarGlassItem.seed(stack) == 7L, "Seed must persist");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void warded_amulet_rolls_exactly_once(GameTestHelper helper) {
        var stack = new ItemStack(CatalogItems.WARDED_AMULET.get());
        helper.assertTrue(
                !com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder.read(stack).rolled(),
                "Fresh amulet must not be rolled");
        var rolled =
                com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder.rollIfNeeded(
                        stack,
                        helper.getLevel().random,
                        CatalogItems.candidateEnchantments(),
                        id -> 0,
                        java.util.UUID.fromString("00000000-0000-0000-0000-0000000000AA"));
        helper.assertTrue(rolled, "First equip must roll modifiers");
        var data = com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder.read(stack);
        helper.assertTrue(data.rolled(), "Rolled flag must persist");
        helper.assertTrue(
                data.modifiers().size() <= 3, "At most three modifiers (§6.4.4)");
        helper.assertTrue(
                !com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder.rollIfNeeded(
                        stack, helper.getLevel().random, CatalogItems.candidateEnchantments(), id -> 0, null),
                "Re-equipping must not re-roll");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void amulet_bridge_boosts_vanilla_enchantment_levels(GameTestHelper helper) {
        var sharpness =
                net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(
                        new ResourceLocation("minecraft", "sharpness"));
        helper.assertTrue(sharpness != null, "Vanilla sharpness must exist");
        var id = new ResourceLocation("minecraft", "sharpness");
        var stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.enchant(sharpness, 1);
        // 无上下文：Mixin 路径必须返回原值。
        helper.assertTrue(
                net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                sharpness, stack)
                        == 1,
                "Without a player context the amulet must not apply");
        com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge.pushContext(
                List.of(
                        new com.mpp.stellaeomphalos.content.item.amulet.AmuletModifier(
                                com.mpp.stellaeomphalos.content.item.amulet.AmuletModifier
                                        .ModifierKind.ADD_SPECIFIC,
                                java.util.Optional.of(id),
                                2)));
        try {
            // 上下文存在：M-1 注入点必须把 +2 叠加到原版读取上。
            helper.assertTrue(
                    net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                    sharpness, stack)
                            == 3,
                    "Amulet bridge must boost the vanilla enchantment level");
            helper.assertTrue(
                    com.mpp.stellaeomphalos.core.platform.GameplayQueries.enchantments(
                                    stack, java.util.Map.of(sharpness, 1))
                            .get(sharpness)
                            == 3,
                    "Batch query path must agree with the single-level path");
        } finally {
            com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge.clearContext();
        }
        helper.assertTrue(
                net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                                sharpness, stack)
                        == 1,
                "Clearing the context must restore the vanilla value");
        helper.succeed();
    }
}
