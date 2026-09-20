package com.mpp.stellaeomphalos.lumen.transport.stasis;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.OmphalosConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class StasisGameTests {
    private static StasisService service(GameTestHelper helper) {
        StasisBootstrap.attach(null);
        var service = StasisService.get(helper.getLevel().getServer());
        service.clear(helper.getLevel());
        return service;
    }

    @GameTest(template = "foundation_empty", timeoutTicks = 200)
    public static void stasis_zone_freezes_entities_but_advances_hurt_time(GameTestHelper helper) {
        var level = helper.getLevel();
        var service = service(helper);
        var center = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(service.activate(level, center, 6, new StasisFilter(StasisFilter.Mode.ALL_EXCEPT, null, true), 400, null),
                "Activation refused");
        helper.runAfterDelay(15, () -> {
            var pig = helper.spawn(EntityType.PIG, 1, 2, 1);
            var item = helper.spawnItem(Items.DIAMOND, 1, 4, 1);
            pig.hurt(level.damageSources().generic(), 1.0F);
            helper.assertTrue(pig.hurtTime == 10, "hurtTime baseline wrong: " + pig.hurtTime);
            var pigAt = pig.position();
            double itemY = item.getY();
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(pig.isAlive(), "Pig died while frozen");
                helper.assertTrue(service.isFrozen(pig), "Pig not recognized as frozen");
                helper.assertTrue(pig.hurtTime < 10, "hurtTime must keep ticking inside stasis, got " + pig.hurtTime);
                helper.assertTrue(pig.position().distanceToSqr(pigAt) < 1.0E-4, "Frozen pig moved: " + pig.position());
                helper.assertTrue(Math.abs(item.getY() - itemY) < 0.25, "Frozen item entity fell: " + item.getY());
                service.clear(level);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "foundation_empty", timeoutTicks = 200)
    public static void stasis_zones_survive_save_reload(GameTestHelper helper) {
        var level = helper.getLevel();
        var service = service(helper);
        var center = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(service.activate(level, center, 5, new StasisFilter(StasisFilter.Mode.NO_PLAYERS, null, false), 300, null),
                "Activation refused");
        var data = StasisData.get(level);
        var tag = data.save(new CompoundTag());
        var restored = new StasisData();
        restored.restore(tag, level.getGameTime());
        helper.assertTrue(restored.state().equals(data.state()), "SavedData roundtrip mismatch");
        helper.assertTrue(restored.state().zones().size() == 1, "Zone lost in roundtrip");
        var record = restored.state().zones().get(0);
        helper.assertTrue(record.remaining() == 300 && record.filterMode().equals("no_players") && record.radius() == 5.0F,
                "Freeze strategy or remaining ticks not preserved");
        service.reload(level);
        var views = service.zones(level);
        helper.assertTrue(views.size() == 1, "Zone not restored after reload");
        helper.assertTrue(views.get(0).center().equals(center) && views.get(0).radius() == 5.0, "Restored zone geometry wrong");
        helper.assertTrue(views.get(0).phase() == StasisZone.Phase.WARMUP && views.get(0).remainingTicks() == 300,
                "Restored zone must re-warm-up with remaining ticks intact");
        service.clear(level);
        helper.succeed();
    }

    @GameTest(template = "foundation_empty", timeoutTicks = 200)
    public static void stasis_oldest_zone_released_over_limit(GameTestHelper helper) {
        var level = helper.getLevel();
        var service = service(helper);
        int max = OmphalosConfig.SERVER.integer("gameplay.stasisMaxZones");
        var first = new BlockPos(100000, 100, 100000);
        for (int i = 0; i < max + 1; i++)
            helper.assertTrue(service.activate(level, first.offset(i * 64, 0, 0), 4,
                    new StasisFilter(StasisFilter.Mode.ALL_EXCEPT, null, true), 100000, null), "Activation refused at " + i);
        var views = service.zones(level);
        helper.assertTrue(views.size() == max, "Zone cap not enforced: " + views.size());
        helper.assertTrue(views.stream().noneMatch(view -> view.center().equals(first)), "Oldest zone survived the cap");
        helper.assertTrue(views.stream().anyMatch(view -> view.center().equals(first.offset(max * 64, 0, 0))), "Newest zone missing");
        service.clear(level);
        helper.succeed();
    }
}
