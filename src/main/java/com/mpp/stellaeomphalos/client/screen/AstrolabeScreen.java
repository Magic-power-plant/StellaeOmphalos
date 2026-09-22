package com.mpp.stellaeomphalos.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Drag a catalogued target to the sky disc; only a server reply supplies direction or coordinates.
 */
public final class AstrolabeScreen extends Screen {
    private static final ResourceLocation[] TARGETS =
            java.util.stream.Stream.of(
                            "ancient_shrine",
                            "small_shrine",
                            "small_ruin",
                            "desert_shrine",
                            "treasure_shrine",
                            "lumen_spring")
                    .map(id -> new ResourceLocation("stellaeomphalos", id))
                    .toArray(ResourceLocation[]::new);
    private final Component[] names = new Component[TARGETS.length];
    private int page, dragging = -1, selected = -1, left, top;
    private Component result = Component.empty();
    private com.mpp.stellaeomphalos.network.toClient.AstrolabeFixPayload previous;

    public AstrolabeScreen() {
        super(Component.translatable("stellaeomphalos.visual.astrolabe"));
        for (int i = 0; i < names.length; i++)
            names[i] = Component.translatable("structure.stellaeomphalos." + TARGETS[i].getPath());
    }

    @Override
    protected void init() {
        left = width / 2 - 140;
        top = Math.max(30, height / 2 - 110);
        addRenderableWidget(
                Button.builder(Component.literal("<"), b -> page = 0)
                        .bounds(left + 178, top + 132, 40, 18)
                        .build());
        addRenderableWidget(
                Button.builder(Component.literal(">"), b -> page = 1)
                        .bounds(left + 230, top + 132, 40, 18)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), b -> onClose())
                        .bounds(width / 2 - 50, Math.min(height - 24, top + 196), 100, 20)
                        .build());
    }

    @Override
    public void tick() {
        if (selected < 0) return;
        var fix = AstrolabeTargetCache.get(TARGETS[selected]);
        if (fix == previous) return;
        previous = fix;
        if (fix != null)
            result =
                    fix.deniedReason() != 0
                            ? Component.translatable("stellaeomphalos.astrolabe.unavailable")
                            : Component.translatable(
                                    "stellaeomphalos.astrolabe.fix",
                                    names[selected],
                                    fix.bearingQ() / 10.0,
                                    fix.distanceBand());
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= left + 172 && x < left + 278 && y >= top + 8 && y < top + 128) {
            int row = (int) (y - top - 8) / 30;
            int index = page * 4 + row;
            if (index < TARGETS.length) {
                dragging = index;
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        int index = dragging;
        dragging = -1;
        if (index >= 0 && Math.hypot(x - left - 78, y - top - 78) <= 66) {
            selected = index;
            previous = null;
            result = Component.translatable("stellaeomphalos.visual.waiting");
            AstrolabeTargetCache.request(TARGETS[index]);
            return true;
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fill(left, top, left + 280, top + 188, 0xff151c30);
        g.drawCenteredString(font, title, width / 2, top - 18, 0xffe8d7b3);
        for (int i = 0; i < 64; i++) {
            double a = i * Math.PI / 32, b = (i + 1) * Math.PI / 32;
            CelestialScreen.line(
                    g,
                    left + 78 + (int) (Math.cos(a) * 66),
                    top + 78 + (int) (Math.sin(a) * 66),
                    left + 78 + (int) (Math.cos(b) * 66),
                    top + 78 + (int) (Math.sin(b) * 66),
                    0xff899dc7);
        }
        for (int row = 0; row < 4; row++) {
            int i = page * 4 + row;
            if (i >= names.length) break;
            g.fill(left + 172, top + 8 + row * 30, left + 278, top + 34 + row * 30, 0xff33415c);
            g.drawString(font, names[i], left + 175, top + 17 + row * 30, 0xffd4dfff);
        }
        if (dragging >= 0) g.drawString(font, names[dragging], mx + 5, my - 5, 0xffe8d7b3);
        if (previous != null && previous.deniedReason() == 0) {
            double a = Math.toRadians(previous.bearingQ() / 10.0);
            CelestialScreen.line(
                    g,
                    left + 78,
                    top + 78,
                    left + 78 - (int) (Math.sin(a) * 52),
                    top + 78 + (int) (Math.cos(a) * 52),
                    0xffaaddff);
        }
        g.drawString(font, result, left + 6, top + 170, 0xffafc5ee);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
