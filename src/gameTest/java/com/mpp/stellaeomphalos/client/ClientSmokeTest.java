package com.mpp.stellaeomphalos.client;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.client.screen.ConfigOverviewScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Omphalos.MODID, value = Dist.CLIENT)
public final class ClientSmokeTest {
    private static int ticks;
    private static boolean opened;

    private ClientSmokeTest() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("stellaeomphalos.smokeTest") || event.phase != TickEvent.Phase.END)
            return;
        var minecraft = Minecraft.getInstance();
        if (Boolean.getBoolean("stellaeomphalos.part7SmokeTest")) {
            ClientRenderSmoke.tick(minecraft);
            return;
        }
        if (minecraft.getOverlay() != null) return;
        if (!opened && minecraft.screen instanceof TitleScreen title) {
            minecraft.setScreen(new ConfigOverviewScreen(title));
            opened = true;
        }
        if (!opened) return;
        if (++ticks == 40)
            Screenshot.grab(
                    minecraft.gameDirectory,
                    "foundation-config.png",
                    minecraft.getMainRenderTarget(),
                    text -> LogUtils.getLogger().info("Smoke screenshot: {}", text.getString()));
        if (Boolean.getBoolean("stellaeomphalos.part5SmokeTest")) {
            CodexClientSmoke.tick(minecraft, ticks);
            return;
        }
        if (ticks == 80) {
            LogUtils.getLogger().info("FOUNDATION_CLIENT_SMOKE_PASS");
            minecraft.stop();
        }
    }
}
