package com.mpp.stellaeomphalos.client.hud;

import com.mpp.stellaeomphalos.client.charge.ChargeMirror;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Charge is normalized by the existing network codec, so pixel fill never overflows. */
public final class LumenBarHud {
    private static final HudFadeState FADE = new HudFadeState();
    private static float previous = -1;

    private LumenBarHud() {}

    public static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float value = ChargeMirror.charge();
        if (value != previous || !mc.player.getMainHandItem().isEmpty()) FADE.request(40);
        previous = value;
        FADE.tick();
    }

    public static void render(GuiGraphics g) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || FADE.alpha() < .02F) return;
        int x = g.guiWidth() / 2 - 91, y = g.guiHeight() - 33;
        int alpha = (int) (FADE.alpha() * 255);
        float fill = mc.player.isCreative() ? 1 : Math.max(0, Math.min(1, ChargeMirror.charge()));
        g.fill(x, y, x + 182, y + 4, (alpha << 24) | 0x172236);
        g.fill(x + 1, y + 1, x + 1 + (int) (fill * 180), y + 3, (alpha << 24) | 0x9dcfff);
    }

    public static void clear() {
        FADE.clear();
        previous = -1;
    }
}
