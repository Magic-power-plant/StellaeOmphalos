package com.mpp.stellaeomphalos.knowledge;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.item.*;
import com.mpp.stellaeomphalos.content.item.knowledge.*;
import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.knowledge.research.*;
import com.mpp.stellaeomphalos.network.*;
import com.mpp.stellaeomphalos.network.toServer.*;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.mantle.*;
import com.mpp.stellaeomphalos.player.progress.*;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

import java.util.*;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class PartFiveGameTests {
    private static ResourceLocation id(String path) {
        return new ResourceLocation(Omphalos.MODID, path);
    }

    private static ServerPlayer player(GameTestHelper h) {
        var p =
                new ServerPlayer(
                        h.getLevel().getServer(),
                        h.getLevel(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "knowledge-test"));
        p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(BlockPos.ZERO)));
        return p;
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void catalogue_pages_items_and_milestones_are_complete(GameTestHelper h) {
        h.assertTrue(
                KnowledgeCatalog.PAGES.pages().size() >= 300,
                "At least 300 original page definitions");
        h.assertTrue(KnowledgeCatalog.SHARDS.all().size() == 110, "110 localized shards");
        for (String name : List.of("codex", "lore_shard", "lore_capsule", "lore_scroll"))
            h.assertTrue(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id(name)),
                    "Knowledge item " + name);
        for (String kind : List.of("sign", "rite", "altar", "shard", "boon", "resonance"))
            h.assertTrue(
                    net.minecraft.advancements.CriteriaTriggers.getCriterion(
                                    id(kind + "_milestone"))
                            != null,
                    "Milestone " + kind);
        for (var node : KnowledgeCatalog.NODES.all())
            for (var page : node.pages())
                h.assertTrue(KnowledgeCatalog.PAGES.find(page).isPresent(), "Page " + page);
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void archive_restores_boons_and_knowledge_from_saveddata(GameTestHelper h) {
        var p = player(h);
        var record = StarRecords.get(p);
        record.promote(StarTier.RADIANCE);
        record.branch("ATTUNEMENT");
        record.node(id("sample"));
        record.shard(id("lore/sample"));
        record.read(id("codex/sample"), "stellaeomphalos:sample#0");
        var boon = BoonProgress.getServer(p);
        boon.discover(p, id("aevitas"));
        boon.grantExp(p, Long.MAX_VALUE);
        var archive = StarRecords.store(p.server).save(new CompoundTag());
        for (int i = 0; i < 10; i++) {
            var store = new StarRecordStore(archive, new StarRecordIO(StarRecordIO.DISK), m -> {});
            h.assertTrue(
                    store.record(p.getUUID()).save().equals(record.save()),
                    "Archive field equality");
            archive = store.save(new CompoundTag());
        }
        p.getPersistentData().remove(BoonProgress.ROOT_KEY);
        BoonProgress.dropCache(p.getUUID());
        h.assertTrue(
                BoonProgress.getServer(p).knownSigns().contains(id("aevitas")),
                "Canonical archive restores absent player NBT");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void fake_players_cannot_gain_progress_or_reveal_items(GameTestHelper h) {
        var p =
                net.minecraftforge.common.util.FakePlayerFactory.get(
                        h.getLevel(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "fake-knowledge"));
        h.assertFalse(StarRecords.ACCESS.promote(p, StarTier.BRILLIANCE), "Fake tier denied");
        h.assertFalse(StarRecords.ACCESS.unlockNode(p, id("node")), "Fake node denied");
        h.assertFalse(StarRecords.ACCESS.collectShard(p, id("shard")), "Fake shard denied");
        h.assertFalse(StarRecords.ACCESS.unlockBranch(p, "RADIANCE"), "Fake branch denied");
        h.assertFalse(StarRecords.ACCESS.recordTarget(p, id("target")), "Fake target denied");
        h.assertFalse(StarRecords.ACCESS.readPage(p, id("page"), "route"), "Fake read denied");
        BoonProgress.getServer(p).discover(p, id("aevitas"));
        BoonProgress.getServer(p).grantExp(p, 100);
        h.assertTrue(
                BoonProgress.getServer(p).knownSigns().isEmpty(), "Fake boon discovery denied");
        h.assertFalse(KnowledgeProtocol.reveal(p, InteractionHand.MAIN_HAND), "Fake reveal denied");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void capsules_replace_in_hand_and_duplicate_reveals_consume_once(
            GameTestHelper h) {
        var p = player(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KnowledgeContent.CAPSULE.get()));
        KnowledgeContent.CAPSULE.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        var shard = p.getMainHandItem();
        h.assertTrue(
                shard.is(KnowledgeContent.SHARD.get()) && LoreShardItem.seeded(shard),
                "Capsule replaced by seeded shard");
        var copy = shard.copy();
        long seed = shard.getTag().getLong("ShardSeed");
        h.assertTrue(
                KnowledgeProtocol.reveal(p, InteractionHand.MAIN_HAND), "First reveal succeeds");
        h.assertTrue(p.getMainHandItem().isEmpty(), "One consumed");
        p.setItemInHand(InteractionHand.MAIN_HAND, copy);
        h.assertFalse(
                KnowledgeProtocol.reveal(p, InteractionHand.MAIN_HAND), "Immediate replay denied");
        h.assertTrue(copy.getTag().getLong("ShardSeed") == seed, "Seed never rewritten");
        h.runAfterDelay(
                5,
                () -> {
                    h.assertTrue(
                            KnowledgeProtocol.reveal(p, InteractionHand.MAIN_HAND),
                            "Duplicate after cooldown consumed");
                    h.assertTrue(
                            StarRecords.get(p).unlockedShards().size() == 1
                                    && p.getMainHandItem().isEmpty(),
                            "No duplicate unlock");
                    h.succeed();
                });
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void unseeded_shards_disappear_in_inventory_and_world(GameTestHelper h) {
        var p = player(h);
        var stack = new ItemStack(KnowledgeContent.SHARD.get());
        stack.inventoryTick(h.getLevel(), p, 0, false);
        h.assertTrue(stack.isEmpty(), "Unseeded inventory shard erased");
        var pos = h.absolutePos(BlockPos.ZERO);
        var entity =
                new ItemEntity(
                        h.getLevel(),
                        pos.getX(),
                        pos.getY() + 2,
                        pos.getZ(),
                        new ItemStack(KnowledgeContent.SHARD.get()));
        h.getLevel().addFreshEntity(entity);
        h.runAfterDelay(
                2,
                () -> {
                    h.assertTrue(entity.isRemoved(), "Unseeded dropped shard erased");
                    h.succeed();
                });
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void capsule_drop_is_blast_proof_with_three_hundred_tick_lifespan(
            GameTestHelper h) {
        var stack = new ItemStack(KnowledgeContent.CAPSULE.get());
        h.assertTrue(stack.getEntityLifespan(h.getLevel()) == 300, "Lifespan 300 ticks");
        h.assertFalse(
                stack.getItem().canBeHurtBy(h.getLevel().damageSources().explosion(null, null)),
                "Explosion-proof capsule");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void crafted_scroll_merges_only_knowledge(GameTestHelper h) {
        var a = player(h);
        var b = player(h);
        var recordA = StarRecords.get(a);
        recordA.promote(StarTier.RADIANCE);
        recordA.discover(id("aevitas"));
        var scroll = new ItemStack(KnowledgeContent.SCROLL.get());
        a.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        KnowledgeContent.SCROLL.get().use(h.getLevel(), a, InteractionHand.MAIN_HAND);
        var before = BoonProgress.getServer(b).exp();
        var points = BoonProgress.getServer(b).availablePoints();
        b.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        KnowledgeContent.SCROLL.get().use(h.getLevel(), b, InteractionHand.MAIN_HAND);
        h.assertTrue(StarRecords.get(b).knownSigns().contains(id("aevitas")), "Knowledge merged");
        h.assertTrue(
                BoonProgress.getServer(b).exp() == before
                        && BoonProgress.getServer(b).availablePoints() == points,
                "Economy unchanged");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void low_progress_requests_do_not_grant_reading_or_preview_access(
            GameTestHelper h) {
        var p = player(h);
        var record = StarRecords.get(p);
        var before = record.save();
        var late =
                KnowledgeCatalog.NODES.all().stream()
                        .filter(n -> n.branch() == StudyBranch.RADIANCE)
                        .findFirst()
                        .orElseThrow();
        h.assertFalse(
                KnowledgeProtocol.read(p, new CodexRoute(late.id(), 0).encode()),
                "Locked page rejected");
        h.assertFalse(
                KnowledgeProtocol.read(p, "stellaeomphalos:missing#0"), "Unknown page rejected");
        h.assertFalse(KnowledgeProtocol.read(p, "bad"), "Malformed route rejected");
        h.assertFalse(
                StructureAccess.progress().canPreview(p, id("pattern_gateway")),
                "No high-level projection");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
        h.assertFalse(
                KnowledgeProtocol.reveal(p, InteractionHand.MAIN_HAND), "Wrong item rejected");
        h.assertTrue(record.save().equals(before), "Rejected requests cannot write authority");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void new_payloads_roundtrip_and_reject_wrong_direction(GameTestHelper h) {
        var registry = OmphalosChannel.PAYLOADS;
        var payload = new PktRevealShard(0);
        var bytes = registry.encode(payload);
        h.assertTrue(
                registry.decode(42, PayloadRegistry.Direction.TO_SERVER, bytes).equals(payload),
                "Reveal request roundtrip");
        boolean denied = false;
        try {
            registry.decode(42, PayloadRegistry.Direction.TO_CLIENT, bytes);
        } catch (IllegalArgumentException e) {
            denied = true;
        }
        h.assertTrue(denied, "Wrong direction denied");
        h.assertTrue(registry.type(39).minimumMinor() == 1, "New protocol requires minor 1");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void mantle_codec_and_live_guardian_state_follow_the_stack(GameTestHelper h) {
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id("mantle"));
        h.assertTrue(item instanceof ArmorItem, "Mantle is wearable chest armor");
        var stack = new ItemStack(item);
        stack.getOrCreateTag().putString("SignId", id("armara").toString());
        var data = new CompoundTag();
        data.putInt("Stacks", 2);
        var state = new MantleState(MantleRegistry.id(MantleRegistry.Kind.GUARDIAN), data);
        stack.getOrCreateTag()
                .put(
                        "MantleState",
                        MantleState.CODEC
                                .encodeStart(NbtOps.INSTANCE, state)
                                .getOrThrow(false, m -> {}));
        var saved = ItemStack.of(stack.save(new CompoundTag()));
        var loaded =
                MantleState.CODEC
                        .parse(NbtOps.INSTANCE, saved.getTag().getCompound("MantleState"))
                        .getOrThrow(false, m -> {});
        h.assertTrue(
                state.equals(loaded), "Effect state survives inventory ownership change and NBT");
        var effect = MantleRegistry.create(loaded, MantleParameters.current());
        var action = new MantleAction(MantleAction.Kind.HURT, 1, 10, false, false);
        effect.perform(action, true);
        h.assertTrue(effect.snapshot().data().getInt("Stacks") == 2, "Simulation retains stack");
        effect.perform(action, false);
        h.assertTrue(effect.snapshot().data().getInt("Stacks") == 1, "Commit consumes exactly one");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void harvest_sampling_exception_restores_real_boon_event_amplification(
            GameTestHelper h) {
        var p = player(h);
        var savedTree = com.mpp.stellaeomphalos.constellation.boon.BoonTree.get();
        var nodeId = id("test/harvest");
        try {
            var tree = new com.mpp.stellaeomphalos.constellation.boon.BoonTree();
            tree.register(
                    new com.mpp.stellaeomphalos.constellation.boon.MajorBoonNode(
                            nodeId,
                            0,
                            0,
                            List.of(
                                    new com.mpp.stellaeomphalos.constellation.attribute
                                            .BoonModifier(
                                            com.mpp.stellaeomphalos.constellation.attribute
                                                    .BoonAttributes.HARVEST_SPEED,
                                            com.mpp.stellaeomphalos.constellation.attribute
                                                    .BoonModifier.Mode.STACKING_MULTIPLY,
                                            2,
                                            false)),
                            List.of(),
                            Set.of(),
                            null));
            tree.freeze();
            com.mpp.stellaeomphalos.constellation.boon.BoonTree.rebuild(tree);
            var tag = new CompoundTag();
            tag.putInt(
                    "BoonTreeVersion",
                    com.mpp.stellaeomphalos.constellation.boon.BoonTree.BOON_TREE_VERSION);
            tag.put("Applied", StarRecord.list(Set.of(nodeId)));
            StarRecords.get(p).captureBoons(tag);
            BoonProgress.dropCache(p.getUUID());
            com.mpp.stellaeomphalos.constellation.attribute.BoonValueBridge.invalidate(p.getUUID());
            try {
                com.mpp.stellaeomphalos.constellation.attribute.BoonAttributeListeners
                        .withoutHarvestSpeedBonus(
                                () -> {
                                    throw new IllegalStateException("injected");
                                });
            } catch (IllegalStateException expected) {
            }
            var event =
                    new net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed(
                            p,
                            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                            3,
                            p.blockPosition());
            com.mpp.stellaeomphalos.constellation.attribute.BoonAttributeListeners.harvestSpeed(
                    event);
            h.assertTrue(
                    event.getNewSpeed() == 6,
                    "Failed sampler must not permanently suppress real harvest bonuses");
            h.succeed();
        } finally {
            com.mpp.stellaeomphalos.constellation.boon.BoonTree.rebuild(savedTree);
            BoonProgress.dropCache(p.getUUID());
            com.mpp.stellaeomphalos.constellation.attribute.BoonValueBridge.invalidate(p.getUUID());
        }
    }

    @GameTest(template = "foundation_empty", batch = "part5")
    public static void research_gates_do_not_hide_recipe_browser_definitions(GameTestHelper h) {
        var p = player(h);
        var late =
                KnowledgeCatalog.NODES.all().stream()
                        .filter(
                                n ->
                                        n.branch() == StudyBranch.RADIANCE
                                                && n.id().getPath().startsWith("recipe/"))
                        .findFirst()
                        .orElseThrow();
        var page = KnowledgeCatalog.PAGES.find(late.pages().get(1)).orElseThrow();
        h.assertTrue(
                h.getLevel()
                        .getRecipeManager()
                        .byKey(new ResourceLocation(page.reference()))
                        .isPresent(),
                "Recipe-browser data remains transparent");
        h.assertFalse(
                late.visibility(KnowledgeProtocol.context(p)).level().readable(),
                "Codex content stays gated");
        h.succeed();
    }
}
