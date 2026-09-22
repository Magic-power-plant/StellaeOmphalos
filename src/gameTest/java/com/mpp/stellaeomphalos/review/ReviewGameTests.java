package com.mpp.stellaeomphalos.review;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.blockentity.crafting.CraftingContent;
import com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity;
import com.mpp.stellaeomphalos.content.world.*;
import com.mpp.stellaeomphalos.constellation.sign.*;
import com.mpp.stellaeomphalos.constellation.starmap.*;
import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.lumen.transport.LumenDistributionBridge;
import com.mpp.stellaeomphalos.network.toServer.PktImprintEngrave;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.structure.match.*;
import com.mpp.stellaeomphalos.structure.pattern.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class ReviewGameTests {
    private static ResourceLocation id(String path) { return new ResourceLocation(Omphalos.MODID, path); }
    private static ServerPlayer player(GameTestHelper h) {
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "review-test"));
        player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)));
        return player;
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void water_query_extension_is_reentrant_and_removable(GameTestHelper h) {
        var cow = h.spawn(EntityType.COW, BlockPos.ZERO);
        java.util.function.Consumer<com.mpp.stellaeomphalos.core.platform.api.WaterMovementQueryEvent> listener = e -> {
            if (e.entity() != cow) return;
            h.assertTrue(Math.abs(com.mpp.stellaeomphalos.core.platform.GameplayQueries.waterSlowdown(cow, .8F) - .8F) < .001F,
                    "Nested query uses vanilla base");
            e.setSlowdown(.4F);
        };
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener(listener);
        try {
            h.assertTrue(Math.abs(com.mpp.stellaeomphalos.core.platform.GameplayQueries.waterSlowdown(cow, .8F) - .4F) < .001F,
                    "Subscriber changes drag");
        } finally { bus.unregister(listener); }
        h.assertTrue(Math.abs(com.mpp.stellaeomphalos.core.platform.GameplayQueries.waterSlowdown(cow, .8F) - .8F) < .001F,
                "No subscriber preserves vanilla drag");
        h.succeed();
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void crystal_tools_have_native_actions_and_attunement_changes_speed(GameTestHelper h) {
        var pick = new ItemStack(CraftingContent.ITEMS.get("geode_pickaxe").get());
        h.assertTrue(pick.getItem() instanceof PickaxeItem && pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Native diamond-tier pickaxe");
        h.assertTrue(CraftingContent.ITEMS.get("geode_axe").get() instanceof AxeItem, "Native axe");
        h.assertTrue(CraftingContent.ITEMS.get("geode_shovel").get() instanceof ShovelItem, "Native shovel");
        float before = pick.getDestroySpeed(Blocks.STONE.defaultBlockState());
        var traits = pick.getOrCreateTagElement("ToolTraits"); traits.putInt("Collect", 25); traits.putInt("MaxCollect", 100);
        h.assertTrue(Math.abs(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) - before * .5F) < .001F, "Quality scales effective digging speed");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(id("altar/upgrade_radiance")).isPresent(), "Highest altar tier is craftable");
        h.succeed();
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void engrave_requires_current_session_dimension_station_and_paper(GameTestHelper h) {
        var p = player(h); var pos = h.absolutePos(BlockPos.ZERO);
        h.getLevel().setBlockAndUpdate(pos, WorldContent.BLOCKS.get("observatory").get().defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENCHANTED_BOOK));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(com.mpp.stellaeomphalos.content.item.PartSixItems.SIGN_CHART.get(), 2));
        BoonProgress.getServer(p).discover(p, id("aevitas"));
        int session = SignSkyService.sessionId(p);
        var strokes = List.of(new PktImprintEngrave.Stroke(SignRegistry.numericId(SignRegistry.byId(id("aevitas"))), 0, 0));
        var handlers = RuntimeServices.current().handlers();
        for (var invalid : List.of(
                new PktImprintEngrave(session + 1, p.level().dimension().location(), pos, 0, strokes),
                new PktImprintEngrave(session, new ResourceLocation("minecraft", "the_nether"), pos, 0, strokes),
                new PktImprintEngrave(session, p.level().dimension().location(), pos.above(20), 0, strokes),
                new PktImprintEngrave(session, p.level().dimension().location(), pos, 1, strokes)))
            handlers.dispatch(p, invalid);
        h.assertTrue(!p.getMainHandItem().hasTag(), "Unauthorized requests cannot mutate held item");
        handlers.dispatch(p, new PktImprintEngrave(session, p.level().dimension().location(), pos, 0, strokes));
        h.assertTrue(SignImprint.read(p.getMainHandItem()).isPresent(), "Valid server handler produces an imprint");
        h.assertTrue(p.getOffhandItem().getCount() == 1, "Exactly one paper consumed");
        h.succeed();
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void imprint_dose_and_death_protection_are_consumed_once(GameTestHelper h) {
        var entity = h.spawn(EntityType.COW, BlockPos.ZERO);
        var stack = new ItemStack(Items.DIAMOND_HELMET);
        new SignImprint(Map.of(id("aevitas"), 1.0), Map.of()).write(stack);
        var effects = new ListTag();
        for (String name : List.of("minecraft:regeneration", "stellaeomphalos:death_protection")) {
            var entry = new CompoundTag(); entry.putString("Id", name); entry.putInt("Duration", 4800); entry.putInt("Amplifier", 0); effects.add(entry);
        }
        stack.getOrCreateTagElement("Imprint").put("Effects", effects);
        h.assertTrue(SignImprint.activateEffects(stack, entity), "Dose applied");
        h.assertTrue(entity.hasEffect(MobEffects.REGENERATION) && entity.hasEffect(StarmapContent.DEATH_PROTECTION.get()), "Both effects reach wearer");
        h.assertTrue(!SignImprint.activateEffects(stack, entity), "Re-equipping cannot refresh the dose");
        var death = new net.minecraftforge.event.entity.living.LivingDeathEvent(entity, entity.damageSources().generic());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(death);
        h.assertTrue(death.isCanceled() && entity.getHealth() > 0 && !entity.hasEffect(StarmapContent.DEATH_PROTECTION.get()), "Cheat death cancels and consumes");
        h.succeed();
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void distribution_bridge_uses_live_sky_and_config_dataset_has_content(GameTestHelper h) {
        var level = h.getLevel();
        for (var sign : SignRegistry.all())
            h.assertTrue(Math.abs(LumenDistributionBridge.distribution(level, sign.id()) - SignSkyService.distribution(level, sign)) < .00001, "Registered distribution provider");
        var data = new com.mpp.stellaeomphalos.network.sync.ServerConfigDataset(); data.refresh(); data.commitTick();
        h.assertTrue(data.version() == 1 && data.fullSnapshot(player(h)).data().contains("progression.maxBoonLevel"), "Real public dataset snapshot");
        h.succeed();
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void technical_defaults_mimic_and_head_tiers_survive_load(GameTestHelper h) {
        var pos = h.absolutePos(BlockPos.ZERO);
        var block = WorldContent.BLOCKS.get("mirage_shell").get();
        var be = new TechnicalBlockEntity(pos, block.defaultBlockState()); be.load(new CompoundTag());
        h.assertTrue(be.hostState().is(Blocks.STONE), "Absent Mimic key retains a solid default");
        be.mimic(Blocks.GOLD_BLOCK.defaultBlockState());
        var copy = new TechnicalBlockEntity(pos, block.defaultBlockState()); copy.load(be.saveWithoutMetadata());
        h.assertTrue(copy.hostState().is(Blocks.GOLD_BLOCK), "Mimic survives reload");
        var core = WorldContent.BLOCKS.get("fountain").get();
        h.getLevel().setBlockAndUpdate(pos, core.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos.below(), com.mpp.stellaeomphalos.content.block.BoreHeadBlock.configure(WorldContent.BORE_HEAD.get().defaultBlockState(), com.mpp.stellaeomphalos.content.block.BoreHeadBlock.BoreMode.LIQUID, com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier.STONE));
        h.getLevel().setBlockAndUpdate(pos.below(2), Blocks.DIAMOND_ORE.defaultBlockState());
        var drill = (TechnicalBlockEntity) h.getLevel().getBlockEntity(pos);
        var tag = drill.saveWithoutMetadata(); tag.putBoolean("Active", true); drill.load(tag);
        for (int n = 0; n < 20; n++) drill.serverTick();
        h.assertTrue(h.getLevel().getBlockState(pos.below(2)).is(Blocks.DIAMOND_ORE), "Stone head cannot harvest diamond ore");
        h.getLevel().setBlockAndUpdate(pos.below(), com.mpp.stellaeomphalos.content.block.BoreHeadBlock.configure(WorldContent.BORE_HEAD.get().defaultBlockState(), com.mpp.stellaeomphalos.content.block.BoreHeadBlock.BoreMode.LIQUID, com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier.DIAMOND));
        tag = drill.saveWithoutMetadata(); tag.putBoolean("Active", true); drill.load(tag);
        for (int n = 0; n < 20; n++) drill.serverTick();
        var wearTag = drill.saveWithoutMetadata();
        h.assertTrue(h.getLevel().getBlockState(pos.below(2)).isAir() && wearTag.getInt("HeadWear") == 1,
                "Diamond head harvests and wears (air=" + h.getLevel().getBlockState(pos.below(2)).isAir()
                        + ", wear=" + wearTag.getInt("HeadWear")
                        + ", boreY=" + wearTag.getInt("BoreY")
                        + ", installed=" + wearTag.getString("InstalledHead")
                        + ", active=" + wearTag.getBoolean("Active")
                        + ", ticks=" + wearTag.getLong("Ticks") + ")");
        h.succeed();
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void pending_retrogen_survives_runtime_recreation_and_unloaded_chunks(GameTestHelper h) {
        var far = new net.minecraft.world.level.ChunkPos(100000, 100000);
        var plan = new RetroGenPlan(h.getLevel()); plan.enqueue(far); int count = plan.queued();
        var restored = new RetroGenPlan(h.getLevel()); restored.tick();
        h.assertTrue(restored.queued() == count, "Recreated plan retains unloaded pending work");
        var saved = h.getLevel().getDataStorage().get(RetroGenPlan.QueueData::load, "stellaeomphalos_retrogen_queue");
        var nbt = saved.save(new CompoundTag());
        h.assertTrue(RetroGenPlan.QueueData.load(nbt).save(new CompoundTag()).equals(nbt), "Pending queue NBT round trip");
        h.succeed();
    }
    @GameTest(template = "foundation_empty", batch = "review")
    public static void placement_enforces_no_paste_and_machine_matching_detects_rotation(GameTestHelper h) {
        var previous = BlueprintRegistry.all(); var pos = h.absolutePos(BlockPos.ZERO);
        var key = id("review_asymmetric");
        var blueprint = PatternBlueprintBuilder.named(key).block(BlockPos.ZERO, Blocks.GOLD_BLOCK)
                .block(new BlockPos(1, 0, 0), Blocks.DIAMOND_BLOCK).build();
        try {
            var next = new HashMap<>(previous); next.put(key, blueprint); BlueprintRegistry.publish(next);
            var result = StructurePlacer.place(blueprint, h.getLevel(), pos, PlacementTransform.CLOCKWISE_90,
                    new PlacementContext(PlacementContext.Source.SCHEMATIC_PASTE, null, true, true, 1));
            h.assertTrue(result.placed() == 2, "Rotated placement succeeds");
            var hub = StructureIntegrityHub.of(h.getLevel());
            h.assertTrue(hub.query(pos, key).canProduce(), "Machine query finds the rotated structure");
            var blocked = PatternBlueprintBuilder.named(id("review_blocked")).block(BlockPos.ZERO, Blocks.EMERALD_BLOCK).noPaste(true).build();
            h.assertTrue(StructurePlacer.place(blocked, h.getLevel(), pos, PlacementTransform.NONE,
                    new PlacementContext(PlacementContext.Source.SCHEMATIC_PASTE, null, true, true, 1)).failed() < 0, "Stateful blueprints reject paste");
            h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.GOLD_BLOCK), "Rejected paste performs no writes");
            hub.release(pos);
        } finally { BlueprintRegistry.publish(previous); }
        h.succeed();
    }
}
