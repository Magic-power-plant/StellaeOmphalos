package com.mpp.stellaeomphalos.constellation.sign;

import com.mpp.stellaeomphalos.Omphalos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Omphalos.MODID)
@PrefixGameTestTemplate(false)
public final class SignGameTests {
    @GameTest(template = "foundation_empty")
    public static void sky_service_tracks_world_time_and_active_signs(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(SignRegistry.all().size() == 16, "Sign registry not assembled from datapack");
        level.setDayTime(6000);
        level.updateSkyBrightness();   // isDay/isNight read the per-tick skyDarken cache; refresh it like a tick would
        helper.assertTrue(level.isDay() && !SignSkyService.isNight(level), "Noon reported as night");
        level.setDayTime(13000);
        level.updateSkyBrightness();
        helper.assertTrue(level.isNight() && SignSkyService.isNight(level), "Midnight not reported as night");
        var active = SignSkyService.activeSigns(level);
        helper.assertTrue(!active.isEmpty(), "Active sign set empty on a real world tick");
        helper.assertTrue(active.size() <= SignSkyScheduler.DAILY_ACTIVE_CAP, "Active sign set exceeds daily cap");
        helper.assertTrue(SignSkyService.distribution(level, active.get(0)) >= 0.0F, "Negative distribution");
        helper.assertTrue(SignSkyService.omenNow(level) != null, "Omen query returned null");
        helper.assertTrue(SignSkyService.dayDistributionFactor(level) < 0.5, "Day factor should drop at night");
        helper.succeed();
    }
}
