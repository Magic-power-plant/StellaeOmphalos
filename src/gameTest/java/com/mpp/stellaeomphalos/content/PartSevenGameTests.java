package com.mpp.stellaeomphalos.content;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.sign.*;
import com.mpp.stellaeomphalos.content.block.PartSixBlocks;
import com.mpp.stellaeomphalos.content.item.PartSixItems;
import com.mpp.stellaeomphalos.content.item.StarGlassItem;
import com.mpp.stellaeomphalos.content.item.knowledge.ObservationProtocol;
import com.mpp.stellaeomphalos.network.toServer.*;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.progress.StarRecords;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;

import java.util.*;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class PartSevenGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var player =
                new ServerPlayer(
                        h.getLevel().getServer(),
                        h.getLevel(),
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "part7-test")) {
                    @Override
                    public void playNotifySound(
                            net.minecraft.sounds.SoundEvent sound,
                            net.minecraft.sounds.SoundSource source,
                            float volume,
                            float pitch) {}

                    @Override
                    public void displayClientMessage(
                            net.minecraft.network.chat.Component text, boolean actionBar) {}
                };
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(BlockPos.ZERO)));
        return player;
    }

    @GameTest(template = "foundation_empty", batch = "part7")
    public static void chart_draw_consumes_one_paper_and_never_overwrites_glass(GameTestHelper h) {
        var player = player(h);
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(pos, PartSixBlocks.STAR_CHART_TABLE.get().defaultBlockState(), 3);
        var machine = (PartSixBlocks.MachineBlockEntity) level.getBlockEntity(pos);
        var selected = SignRegistry.all().subList(0, 3);
        var progress = StarRecords.get(player);
        var strokes = new ArrayList<PktImprintEngrave.Stroke>();
        for (int i = 0; i < 3; i++) {
            progress.discover(selected.get(i).id());
            strokes.add(
                    new PktImprintEngrave.Stroke(
                            SignRegistry.numericId(selected.get(i)), i * 8, i * 8));
        }
        var data = machine.saveWithoutMetadata();
        data.putInt("Paper", 2);
        machine.load(data);
        machine.inventory().setStackInSlot(1, new ItemStack(PartSixItems.STAR_GLASS.get()));
        h.assertTrue(machine.parchment() == 2, "Fixture paper restored");
        boolean completed = false;
        for (int i = 0; i < 10 && !completed; i++) {
            data = machine.saveWithoutMetadata();
            data.putInt("Paper", 2);
            machine.load(data);
            h.assertTrue(machine.drawChart(player, strokes), "Valid drawing accepted");
            h.assertTrue(machine.parchment() == 1, "Exactly one parchment consumed even on burn");
            completed = !StarGlassItem.chart(machine.glass()).isEmpty();
        }
        h.assertTrue(completed, "At least one deterministic seeded draw succeeds");
        h.assertTrue(
                StarGlassItem.chart(machine.glass()).size() == 3, "Three sign ids stored in glass");
        h.assertFalse(
                machine.drawChart(player, strokes), "Duplicate in-progress submission rejected");
        h.assertTrue(machine.parchment() == 1, "Duplicate consumes nothing");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part7")
    public static void telescope_rotation_has_eight_static_states(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        var level = h.getLevel();
        level.setBlock(pos, PartSixBlocks.SPYGLASS.get().defaultBlockState(), 3);
        var machine = (PartSixBlocks.MachineBlockEntity) level.getBlockEntity(pos);
        for (int i = 0; i < 8; i++) {
            machine.setRotation(i);
            h.assertTrue(
                    level.getBlockState(pos).getValue(PartSixBlocks.ROTATION) == i,
                    "Rotation blockstate synchronized");
        }
        machine.setRotation(8);
        h.assertTrue(machine.rotation() == 0, "Rotation wraps");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part7")
    public static void observation_rejects_stale_session_and_missing_instrument(GameTestHelper h) {
        var player = player(h);
        var sign = SignRegistry.majorSigns().get(0);
        var proof =
                sign.lines().stream()
                        .map(
                                e ->
                                        new PktObserveSign.Edge(
                                                e.a().x(), e.a().y(), e.b().x(), e.b().y()))
                        .toList();
        var packet =
                new PktObserveSign(
                        Integer.MIN_VALUE,
                        player.level().dimension().location(),
                        player.blockPosition(),
                        true,
                        sign.id(),
                        proof);
        h.assertFalse(ObservationProtocol.observe(player, packet), "Old connection proof rejected");
        packet =
                new PktObserveSign(
                        SignSkyService.sessionId(player),
                        player.level().dimension().location(),
                        player.blockPosition(),
                        true,
                        sign.id(),
                        proof);
        h.assertFalse(
                ObservationProtocol.observe(player, packet),
                "No instrument or sky gate cannot grant knowledge");
        h.assertFalse(
                BoonProgress.getServer(player).knowsSign(sign.id()),
                "Rejected observation leaves progress intact");
        h.succeed();
    }

    @GameTest(template = "foundation_empty", batch = "part7", timeoutTicks = 80)
    public static void observation_discovers_only_a_seen_active_complete_sign(GameTestHelper h) {
        var player = player(h);
        var level = h.getLevel();
        long previous = level.getDayTime();
        level.setDayTime(18000);
        level.setWeatherParameters(6000, 0, false, false);
        level.updateSkyBrightness();
        player.setPos(player.getX(), 200, player.getZ());
        player.setItemInHand(
                InteractionHand.MAIN_HAND, new ItemStack(PartSixItems.HAND_SPYGLASS.get()));
        h.runAfterDelay(
                2,
                () -> {
                    try {
                        var active =
                                SignSkyService.activeSigns(level).stream()
                                        .sorted(
                                                java.util.Comparator.comparing(
                                                        s -> s instanceof MajorSign ? 0 : 1))
                                        .toList();
                        var sign =
                                active.stream()
                                        .filter(MajorSign.class::isInstance)
                                        .findFirst()
                                        .orElseThrow();
                        var anchor = SignSkyAnchorTable.layout(active).get(sign);
                        double x = anchor.baseX() + 15 * (anchor.incUx() + anchor.incVx()),
                                y = anchor.baseY() + 15 * (anchor.incUy() + anchor.incVy()),
                                z = anchor.baseZ() + 15 * (anchor.incUz() + anchor.incVz());
                        player.setYRot((float) Math.toDegrees(Math.atan2(-x, z)));
                        player.setXRot((float) -Math.toDegrees(Math.atan2(y, Math.hypot(x, z))));
                        var progress = BoonProgress.getServer(player);
                        progress.markSeen(player, sign.id());
                        var proof =
                                sign.lines().stream()
                                        .map(
                                                e ->
                                                        new PktObserveSign.Edge(
                                                                e.a().x(), e.a().y(), e.b().x(),
                                                                e.b().y()))
                                        .toList();
                        var packet =
                                new PktObserveSign(
                                        SignSkyService.sessionId(player),
                                        level.dimension().location(),
                                        player.blockPosition(),
                                        true,
                                        sign.id(),
                                        proof);
                        h.assertTrue(
                                ObservationProtocol.observe(player, packet),
                                "Complete aimed night-time proof accepted");
                        h.assertTrue(progress.knowsSign(sign.id()), "Discovery persisted");
                        h.assertFalse(
                                ObservationProtocol.observe(player, packet),
                                "Replayed proof is idempotent");
                        h.succeed();
                    } finally {
                        level.setDayTime(previous);
                        level.updateSkyBrightness();
                    }
                });
    }

    @GameTest(template = "foundation_empty", batch = "part7")
    public static void gateway_ledger_self_heals_and_rejects_forged_session(GameTestHelper h) {
        var level = h.getLevel();
        var ledger = com.mpp.stellaeomphalos.content.world.GateLedger.get(level);
        var a = h.absolutePos(new BlockPos(1, 1, 1));
        var b = h.absolutePos(new BlockPos(2, 1, 1));
        var gate = com.mpp.stellaeomphalos.content.world.WorldContent.BLOCKS.get("gate_core").get();
        level.setBlock(a, gate.defaultBlockState(), 3);
        level.setBlock(b, gate.defaultBlockState(), 3);
        ledger.add(a);
        ledger.add(b);
        h.assertTrue(ledger.destinations(level, a).contains(b), "Loaded target listed");
        level.setBlock(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        h.assertFalse(ledger.destinations(level, a).contains(b), "Destroyed target pruned");
        h.assertFalse(
                com.mpp.stellaeomphalos.content.world.GateNetworkService.request(
                        player(h), new PktGatewayTravel(0, a, false)),
                "Arbitrary packet cannot create journey");
        h.succeed();
    }

    @GameTest(template="foundation_empty",batch="part7")
    public static void visual_living_entities_have_attributes_and_spawn(GameTestHelper h) {
        var types=java.util.List.of(com.mpp.stellaeomphalos.content.entity.p6.PartSixEntities.WISP.get(),
                com.mpp.stellaeomphalos.content.entity.p6.PartSixEntities.LUMEN_DROPLET.get(),
                com.mpp.stellaeomphalos.content.entity.p6.PartSixEntities.PHANTOM_TOOL.get());
        int i=0;
        for(var type:types){
            var entity=type.create(h.getLevel());h.assertTrue(entity!=null,"Factory creates living visual entity");
            h.assertTrue(entity.getMaxHealth()>0,"Default health registered");
            entity.setPos(h.absolutePos(new BlockPos(++i,2,1)).getX(),h.absolutePos(BlockPos.ZERO).getY()+2,h.absolutePos(BlockPos.ZERO).getZ()+1);
            h.assertTrue(h.getLevel().addFreshEntity(entity),"Entity can enter real level");entity.discard();
        }
        h.succeed();
    }
}
