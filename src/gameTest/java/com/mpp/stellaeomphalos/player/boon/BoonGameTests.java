package com.mpp.stellaeomphalos.player.boon;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.attribute.AttributeBootstrap;
import com.mpp.stellaeomphalos.constellation.attribute.BoonAttributes;
import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonValueBridge;
import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.constellation.boon.BoonUnlockRules;
import com.mpp.stellaeomphalos.constellation.boon.CoreRootBoonNode;
import com.mpp.stellaeomphalos.constellation.boon.MajorBoonNode;
import com.mpp.stellaeomphalos.constellation.boon.RootBoonNode;
import com.mpp.stellaeomphalos.constellation.boon.SocketBoonNode;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Boon module game tests. The tree under test is synthetic (built and swapped in directly), so
 * the tests run both before and after the integrator wires BoonBootstrap into the mod constructor.
 * Bootstraps attach idempotently, following the ChargeGameTests pattern.
 */
@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class BoonGameTests {

    private static final ResourceLocation SIGN = new ResourceLocation(Omphalos.MODID, "aevitas");
    private static final List<ResourceLocation> MAJORS = List.of(
            new ResourceLocation(Omphalos.MODID, "aevitas"), new ResourceLocation(Omphalos.MODID, "armara"),
            new ResourceLocation(Omphalos.MODID, "discidia"), new ResourceLocation(Omphalos.MODID, "evorsio"),
            new ResourceLocation(Omphalos.MODID, "vicio"));
    private static final ResourceLocation ROOT = new ResourceLocation(Omphalos.MODID, "gametest/root");
    private static final ResourceLocation VITALITY = new ResourceLocation(Omphalos.MODID, "gametest/vitality");
    private static final ResourceLocation SOCKET = new ResourceLocation(Omphalos.MODID, "gametest/socket");

    private static ServerPlayer mockPlayer(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "boon-test-player")) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(BlockPos.ZERO)));
        return player;
    }

    /** Synthetic tree: core root + sign root + a max-health major + a socket node. */
    private static void assembleTestTree() {
        AttributeBootstrap.attach(null);   // registers the vanilla-bridged attributes (max_health)
        BoonBootstrap.attach(null);        // bridge provider, discovery source, grid lookup
        var tree = new BoonTree();
        tree.register(new CoreRootBoonNode(BoonUnlockRules.CORE_ROOT_ID, 0, 0, List.of(), List.of(), null));
        tree.registerRoot(SIGN, new RootBoonNode(ROOT, 10, 0, SIGN, 1.0, List.of(), List.of(), null));
        tree.register(new MajorBoonNode(VITALITY, 11, 0,
                List.of(new BoonModifier(BoonAttributes.maxHealth(), BoonModifier.Mode.ADDITION, 6.0, false)),
                List.of(), Set.of(), null));
        tree.register(new SocketBoonNode(SOCKET, 12, 0, List.of(), List.of(), Set.of(), null));
        tree.connect(tree.node(ROOT), tree.node(VITALITY));
        tree.connect(tree.node(VITALITY), tree.node(SOCKET));
        tree.freeze();
        BoonTree.rebuild(tree);
    }

    private static void unlockPrerequisites(ServerPlayer player, BoonProgress progress) {
        MAJORS.forEach(sign -> progress.discover(player, sign));
    }

    @GameTest(template = "foundation_empty", timeoutTicks = 100)
    public static void boon_unlock_is_server_authoritative(GameTestHelper helper) {
        assembleTestTree();
        var player = mockPlayer(helper);
        try {
            var progress = BoonProgress.getServer(player);
            helper.assertFalse(progress.unlock(player, VITALITY), "Unlock without any prerequisite must be refused");
            progress.discover(player, SIGN);
            helper.assertFalse(progress.unlock(player, ROOT), "Root requires the core root first");
            unlockPrerequisites(player, progress);
            helper.assertTrue(progress.unlock(player, BoonUnlockRules.CORE_ROOT_ID), "Core root refused");
            helper.assertTrue(progress.freePointTokens().size() == CoreRootBoonNode.BONUS_TOKENS.size(),
                    "Core root must grant exactly three fixed tokens");
            helper.assertTrue(progress.availablePoints() == 1 + 3, "Level 1 with 3 tokens yields 4 points");
            helper.assertTrue(progress.unlock(player, ROOT), "Root refused after core");
            helper.assertTrue(progress.availablePoints() == 4, "Roots must not consume skill points");
            helper.assertTrue(progress.unlock(player, VITALITY), "Major node refused with adjacent root and points");
            helper.assertTrue(progress.availablePoints() == 3, "Major node must consume one point");
            helper.assertFalse(progress.unlock(player, VITALITY), "Double unlock must be refused");
            helper.assertFalse(progress.unlock(player, new ResourceLocation(Omphalos.MODID, "gametest/missing")),
                    "Unknown node must be refused");
        } finally {
            BoonProgress.dropCache(player.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_empty", timeoutTicks = 100)
    public static void boon_respec_returns_socketed_gem_even_with_full_inventory(GameTestHelper helper) {
        assembleTestTree();
        var player = mockPlayer(helper);
        try {
            var progress = BoonProgress.getServer(player);
            unlockPrerequisites(player, progress);
            progress.unlock(player, BoonUnlockRules.CORE_ROOT_ID);
            progress.unlock(player, ROOT);
            progress.unlock(player, VITALITY);
            helper.assertTrue(progress.unlock(player, SOCKET), "Socket node refused");
            helper.assertFalse(progress.socket(player, SOCKET, new ItemStack(Items.STONE)), "Non-gem must be rejected");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 2));
            helper.assertTrue(com.mpp.stellaeomphalos.content.item.knowledge.KnowledgeProtocol.socketHeld(player, SOCKET), "Server-held gem accepted");
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Exactly one gem consumed");
            helper.assertFalse(com.mpp.stellaeomphalos.content.item.knowledge.KnowledgeProtocol.socketHeld(player, SOCKET), "Occupied socket refuses overwrite");
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Rejected request consumes nothing");
            var node = (SocketBoonNode) BoonTree.get().node(SOCKET);
            helper.assertTrue(node.contained(progress.nodeData(SOCKET)).is(Items.EMERALD), "Gem was not socketed");
            // Fill every main inventory slot so the returned gem cannot fit.
            for (int slot = 0; slot < player.getInventory().items.size(); slot++)
                player.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
            helper.assertTrue(progress.reset(player), "Respec refused");
            helper.assertTrue(node.contained(progress.nodeData(SOCKET)).isEmpty(), "Socket was not cleared");
            helper.assertTrue(!progress.hasNode(SOCKET), "Respec must clear applied nodes");
            var dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(6.0), entity -> entity.getItem().is(Items.EMERALD));
            helper.assertFalse(dropped.isEmpty(), "AC-2.17: gem vanished instead of dropping at the player's feet");
        } finally {
            BoonProgress.dropCache(player.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "foundation_empty", timeoutTicks = 100)
    public static void boon_unlocked_node_flows_through_value_bridge(GameTestHelper helper) {
        assembleTestTree();
        var player = mockPlayer(helper);
        try {
            var progress = BoonProgress.getServer(player);
            double before = BoonValueBridge.value(player, BoonAttributes.maxHealth());
            unlockPrerequisites(player, progress);
            progress.unlock(player, BoonUnlockRules.CORE_ROOT_ID);
            progress.unlock(player, ROOT);
            progress.unlock(player, VITALITY);
            double after = BoonValueBridge.value(player, BoonAttributes.maxHealth());
            helper.assertTrue(after > before, "Unlocked max_health node did not reach the bridge: " + before + " -> " + after);
            helper.assertTrue(Math.abs(after - before - 6.0) < 1.0E-6,
                    "Bridge delta must equal the node modifier: " + (after - before));
            // Sealing the node removes its contribution without consuming the slot's history.
            helper.assertTrue(progress.setSealed(player, VITALITY, true), "Seal refused");
            helper.assertTrue(Math.abs(BoonValueBridge.value(player, BoonAttributes.maxHealth()) - before) < 1.0E-6,
                    "Sealed node must stop contributing");
        } finally {
            BoonProgress.dropCache(player.getUUID());
        }
        helper.succeed();
    }
}
