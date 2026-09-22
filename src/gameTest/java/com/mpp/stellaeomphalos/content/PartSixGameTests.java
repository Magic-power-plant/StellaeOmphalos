package com.mpp.stellaeomphalos.content;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.block.DecorContent;
import com.mpp.stellaeomphalos.content.block.DecorFamily;
import com.mpp.stellaeomphalos.content.block.DecorFamily.DecorVariant;
import com.mpp.stellaeomphalos.content.block.DecorFamilyBlock;
import com.mpp.stellaeomphalos.content.world.WorldContent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Part-6 §6.2.1 注册总账与 §6.3.2 / §6.3.4 行为的运行期验收（AC-6.1 / AC-6.2 / AC-6.3）。 */
@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class PartSixGameTests {

    private static final List<String> BLOCK_LEDGER =
            List.of(
                    "marble",
                    "black_marble",
                    "infused_wood",
                    "marble_slab",
                    "marble_double_slab",
                    "marble_stairs",
                    "spyglass",
                    "quern",
                    "fountain",
                    "bore_head",
                    "lumen_infuser",
                    "star_chart_table",
                    "observatory",
                    "chalice",
                    "grove_beacon",
                    "luminaire",
                    "gate_node",
                    "rite_link",
                    "celestial_orrery",
                    "geode_ore",
                    "aquamarine_sand",
                    "sky_crystal_cluster",
                    "prism_crystal_cluster",
                    "glowbloom",
                    "cosmetic_rock",
                    "frame_shell",
                    "phase_barrier",
                    "proxy_foliage",
                    "mirage_shell",
                    "rupture_anchor",
                    "glow_mote",
                    "ephemeral_light",
                    "asterism_altar",
                    "beam_lens",
                    "beam_prism",
                    "beam_relay",
                    "collector",
                    "lumen_well",
                    "resonance_altar",
                    "rite_pedestal",
                    "constellation_frame");

    @GameTest(template = "foundation_empty")
    public static void block_ledger_is_registered(GameTestHelper helper) {
        for (String id : BLOCK_LEDGER)
            helper.assertTrue(
                    BuiltInRegistries.BLOCK.containsKey(new ResourceLocation(Omphalos.MODID, id)),
                    "Missing Part-6 block registration: " + id);
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void geode_ore_exposes_two_variants(GameTestHelper helper) {
        var ore = WorldContent.GEODE_ORE.get();
        helper.assertTrue(
                ore instanceof com.mpp.stellaeomphalos.content.block.GeodeOreBlock,
                "geode_ore must be the variant-carrying block");
        helper.assertTrue(
                ore.defaultBlockState()
                                .getValue(
                                        com.mpp.stellaeomphalos.content.block.GeodeOreBlock
                                                .VARIANT)
                        == com.mpp.stellaeomphalos.content.block.GeodeOreBlock.Variant.GEODE,
                "Default variant must be GEODE");
        helper.assertTrue(
                WorldContent.astralOreState()
                                .getValue(
                                        com.mpp.stellaeomphalos.content.block.GeodeOreBlock
                                                .VARIANT)
                        == com.mpp.stellaeomphalos.content.block.GeodeOreBlock.Variant.ASTRAL,
                "astralOreState must yield the ASTRAL variant");
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.block.GeodeOreBlock.isAstral(
                        WorldContent.astralOreState()),
                "isAstral must recognise the ASTRAL variant");
        helper.assertTrue(
                !com.mpp.stellaeomphalos.content.block.GeodeOreBlock.isAstral(
                        ore.defaultBlockState()),
                "isAstral must reject the GEODE variant");
        helper.assertTrue(
                !BuiltInRegistries.BLOCK.containsKey(
                        new ResourceLocation(Omphalos.MODID, "star_metal_ore")),
                "star_metal_ore must no longer exist as a separate block");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void bore_head_is_one_block_with_two_modes(GameTestHelper helper) {
        var head = WorldContent.BORE_HEAD.get();
        helper.assertTrue(
                head instanceof com.mpp.stellaeomphalos.content.block.BoreHeadBlock,
                "bore_head must be the mode-carrying block");
        helper.assertTrue(
                head.defaultBlockState()
                                .getValue(com.mpp.stellaeomphalos.content.block.BoreHeadBlock.MODE)
                        == com.mpp.stellaeomphalos.content.block.BoreHeadBlock.BoreMode.LIQUID,
                "Default mode must be LIQUID");
        var vortex =
                com.mpp.stellaeomphalos.content.block.BoreHeadBlock.configure(
                        head.defaultBlockState(),
                        com.mpp.stellaeomphalos.content.block.BoreHeadBlock.BoreMode.VORTEX,
                        com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier.DIAMOND);
        helper.assertTrue(
                vortex.getValue(com.mpp.stellaeomphalos.content.block.BoreHeadBlock.MODE)
                        == com.mpp.stellaeomphalos.content.block.BoreHeadBlock.BoreMode.VORTEX,
                "configure must switch the mode");
        helper.assertTrue(
                vortex.getValue(com.mpp.stellaeomphalos.content.block.BoreHeadBlock.TIER)
                        == com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier.DIAMOND,
                "configure must switch the tier");
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.block.BoreHeadBlock.toolFor(
                                com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier.DIAMOND)
                        == Items.DIAMOND_PICKAXE,
                "Tier must map to the matching vanilla tool");
        for (var tier : com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier.values())
            helper.assertTrue(
                    BuiltInRegistries.ITEM.containsKey(
                            new ResourceLocation(
                                    Omphalos.MODID, "bore_head_" + tier.getSerializedName())),
                    "Missing bore head item for tier " + tier);
        helper.assertTrue(
                !BuiltInRegistries.BLOCK.containsKey(
                        new ResourceLocation(Omphalos.MODID, "bore_head_stone")),
                "bore_head_stone must not exist as a block");
        // 回归守护：方块标签里出现已删除的 id 会让整张 tag 加载失败。
        helper.assertTrue(
                head.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),
                "bore_head must be tagged as pickaxe-mineable");
        helper.assertTrue(
                net.minecraft.world.level.block.Blocks.DIAMOND_ORE
                        .defaultBlockState()
                        .is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),
                "The pickaxe tag must still load (an unknown entry empties the whole tag)");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void part_six_menus_are_registered_and_validate_the_station(GameTestHelper helper) {
        for (String id : com.mpp.stellaeomphalos.content.menu.PartSixMenus.ids())
            helper.assertTrue(
                    net.minecraft.core.registries.BuiltInRegistries.MENU.containsKey(
                            new ResourceLocation(Omphalos.MODID, id)),
                    "Missing Part-6 menu registration: " + id);
        var player =
                new net.minecraft.server.level.ServerPlayer(
                        helper.getLevel().getServer(),
                        helper.getLevel(),
                        new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "part6-menu"));
        var pos = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        // 方块缺失时容器必须判定失效。
        helper.assertTrue(
                !com.mpp.stellaeomphalos.content.menu.PartSixMenus.stillValid(
                        player, pos, () -> WorldContent.BLOCKS.get("observatory").get()),
                "A missing station must invalidate the menu");
        helper.setBlock(new BlockPos(2, 1, 2), WorldContent.BLOCKS.get("observatory").get());
        helper.assertTrue(
                com.mpp.stellaeomphalos.content.menu.PartSixMenus.stillValid(
                        player, pos, () -> WorldContent.BLOCKS.get("observatory").get()),
                "A present station within reach must stay valid");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void rite_links_pair_both_ways_and_refuse_a_linked_peer(GameTestHelper helper) {
        var linkBlock = WorldContent.BLOCKS.get("rite_link").get();
        var first = helper.absolutePos(new BlockPos(2, 1, 2));
        var second = helper.absolutePos(new BlockPos(4, 1, 2));
        var third = helper.absolutePos(new BlockPos(6, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 2), linkBlock);
        helper.setBlock(new BlockPos(4, 1, 2), linkBlock);
        helper.setBlock(new BlockPos(6, 1, 2), linkBlock);
        var level = helper.getLevel();
        var a = (com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity) level.getBlockEntity(first);
        var b = (com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity) level.getBlockEntity(second);
        var c = (com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity) level.getBlockEntity(third);
        helper.assertTrue(a != null && b != null && c != null, "Link block entities must exist");

        // 双向写入（§6.6.1）。
        helper.assertTrue(a.tryLink(second), "First pairing must succeed");
        helper.assertTrue(second.equals(a.linkedTo()), "Origin must point at the peer");
        helper.assertTrue(first.equals(b.linkedTo()), "Peer must point back at the origin");

        // `tryLink` 要求对端尚未链接。
        helper.assertTrue(!a.tryLink(third), "An already linked origin must refuse a second link");
        helper.assertTrue(!c.tryLink(first), "An already linked peer must refuse to link");
        helper.assertTrue(c.linkedTo() == null, "A refused link must not write any state");
        helper.assertTrue(!a.tryLink(first), "Self-linking must be refused");

        // 拆除一端会清掉对端的链接，避免悬空引用。
        level.removeBlock(second, false);
        helper.assertTrue(a.linkedTo() == null, "Removing one end must clear the peer's link");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void decorated_pillars_connect_both_ways(GameTestHelper helper) {
        var block = DecorContent.family(DecorFamily.MARBLE).get();
        var base = new BlockPos(2, 1, 2);
        var pillar = block.defaultBlockState().setValue(DecorFamilyBlock.VARIANT, DecorVariant.PILLAR);
        helper.setBlock(base, pillar);
        helper.setBlock(base.above(), pillar);
        var lower = helper.getBlockState(base);
        var upper = helper.getBlockState(base.above());
        helper.assertTrue(
                lower.getValue(DecorFamilyBlock.TOP) && !lower.getValue(DecorFamilyBlock.BOTTOM),
                "Lower pillar did not connect upward");
        helper.assertTrue(
                !upper.getValue(DecorFamilyBlock.TOP) && upper.getValue(DecorFamilyBlock.BOTTOM),
                "Upper pillar did not connect downward");
        helper.assertTrue(
                block.defaultBlockState().getValue(DecorFamilyBlock.VARIANT) == DecorVariant.RAW,
                "Family block default variant is not RAW");
        helper.succeed();
    }

    @GameTest(template = "foundation_empty")
    public static void technical_blocks_follow_collision_and_light_table(GameTestHelper helper) {
        var level = helper.getLevel();
        var barrier = WorldContent.BLOCKS.get("phase_barrier").get();
        var anchor = WorldContent.BLOCKS.get("rupture_anchor").get();
        var mote = WorldContent.BLOCKS.get("glow_mote").get();
        var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, barrier);
        helper.assertTrue(
                !barrier.defaultBlockState()
                        .getCollisionShape(level, pos, CollisionContext.empty())
                        .isEmpty(),
                "Phase barrier must keep full collision (§6.3.4)");
        helper.assertTrue(
                barrier.defaultBlockState()
                        .getShape(level, pos, CollisionContext.empty())
                        .isEmpty(),
                "Phase barrier selection box must degenerate to zero");
        helper.assertTrue(anchor.defaultBlockState().getLightEmission() == 3, "Anchor light != 3");
        helper.assertTrue(mote.defaultBlockState().getLightEmission() == 15, "Mote light != 15");
        helper.assertTrue(
                mote.defaultBlockState()
                        .getCollisionShape(level, pos, CollisionContext.empty())
                        .isEmpty(),
                "Light mote must have no collision");
        helper.succeed();
    }
}
