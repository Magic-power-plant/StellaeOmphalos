package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.content.menu.StationMenus;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Vanilla container coordinates are preserved; observing opens a returnable canvas above the menu.
 */
public final class StationScreens {
    private StationScreens() {}

    public static class Station<T extends StationMenus.StationMenu>
            extends AbstractContainerScreen<T> {
        private boolean observationOpen;

        public void resumeObservation() {
            observationOpen = false;
        }

        private void observe() {
            observationOpen = true;
            minecraft.setScreen(
                    new CelestialScreen(
                            this,
                            menu instanceof StationMenus.SpyglassMenu
                                    ? CelestialScreen.Mode.TELESCOPE
                                    : CelestialScreen.Mode.OBSERVATORY));
        }

        @Override
        public void removed() {
            if (!observationOpen) super.removed();
        }

        public Station(T menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            imageWidth = 176;
            imageHeight = 166;
        }

        @Override
        protected void init() {
            super.init();
            addRenderableWidget(
                    Button.builder(
                                    Component.translatable("stellaeomphalos.visual.observe"),
                                    b -> observe())
                            .bounds(leftPos + 48, topPos + 56, 80, 18)
                            .build());
            if (menu instanceof StationMenus.SpyglassMenu) {
                addRenderableWidget(
                        Button.builder(Component.literal("<"), b -> rotate(0))
                                .bounds(leftPos + 22, topPos + 56, 20, 18)
                                .build());
                addRenderableWidget(
                        Button.builder(Component.literal(">"), b -> rotate(1))
                                .bounds(leftPos + 134, topPos + 56, 20, 18)
                                .build());
            }
        }

        private void rotate(int direction) {
            if (minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction);
        }

        @Override
        protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
            g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff151d31);
            for (var slot : menu.slots) {
                g.fill(
                        leftPos + slot.x - 1,
                        topPos + slot.y - 1,
                        leftPos + slot.x + 17,
                        topPos + slot.y + 17,
                        0xff8d80a0);
                g.fill(
                        leftPos + slot.x,
                        topPos + slot.y,
                        leftPos + slot.x + 16,
                        topPos + slot.y + 16,
                        0xff25314a);
            }
        }

        @Override
        public void render(GuiGraphics g, int x, int y, float p) {
            renderBackground(g);
            super.render(g, x, y, p);
            renderTooltip(g, x, y);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    public static final class Spyglass extends Station<StationMenus.SpyglassMenu> {
        public Spyglass(StationMenus.SpyglassMenu m, Inventory i, Component t) {
            super(m, i, t);
        }
    }

    public static final class Observatory extends Station<StationMenus.ObservatoryMenu> {
        public Observatory(StationMenus.ObservatoryMenu m, Inventory i, Component t) {
            super(m, i, t);
        }
    }

    public static final class Chart extends Station<StationMenus.StarChartTableMenu> {
        private final java.util.List<net.minecraft.resources.ResourceLocation> signs =
                new java.util.ArrayList<>();
        private final java.util.List<
                        com.mpp.stellaeomphalos.network.toServer.PktImprintEngrave.Stroke>
                strokes = new java.util.ArrayList<>();
        private int selected;
        private boolean dragging;
        private Button submit;

        public Chart(StationMenus.StarChartTableMenu m, Inventory i, Component t) {
            super(m, i, t);
            imageWidth = 336;
        }

        @Override
        protected void init() {
            // Keep the same screen and container while drawing: no orphaned menu or inventory
            // slots.
            super.init();
            clearWidgets();
            signs.clear();
            signs.addAll(
                    com.mpp.stellaeomphalos.client.codex.ClientKnowledgeCache.record()
                            .knownSigns()
                            .stream()
                            .sorted()
                            .toList());
            selected = Math.min(selected, Math.max(0, signs.size() - 1));
            addRenderableWidget(
                    Button.builder(
                                    selectedName(),
                                    b -> {
                                        if (!signs.isEmpty()) {
                                            selected = (selected + 1) % signs.size();
                                            b.setMessage(selectedName());
                                        }
                                    })
                            .bounds(leftPos + 180, topPos + 5, 148, 20)
                            .build());
            addRenderableWidget(
                    Button.builder(
                                    Component.translatable("stellaeomphalos.imprint.clear"),
                                    b -> {
                                        strokes.clear();
                                        submit.active = false;
                                    })
                            .bounds(leftPos + 180, topPos + 143, 64, 18)
                            .build());
            submit =
                    addRenderableWidget(
                            Button.builder(
                                            Component.translatable(
                                                    "stellaeomphalos.imprint.engrave"),
                                            b -> submit())
                                    .bounds(leftPos + 248, topPos + 143, 80, 18)
                                    .build());
            submit.active = strokes.size() == 3;
        }

        private Component selectedName() {
            return signs.isEmpty()
                    ? Component.translatable("stellaeomphalos.visual.no_signs")
                    : Component.translatable(
                            "sign."
                                    + signs.get(selected).getNamespace()
                                    + "."
                                    + signs.get(selected).getPath());
        }

        private void submit() {
            if (strokes.size() == 3
                    && com.mpp.stellaeomphalos.client.OmphalosClient.sendDependent(
                            new com.mpp.stellaeomphalos.network.toServer.PktChartDraw(
                                    menu.containerId, menu.pos(), strokes))) {
                strokes.clear();
                submit.active = false;
            }
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) {
            if (button == 0
                    && !signs.isEmpty()
                    && x >= leftPos + 180
                    && x < leftPos + 328
                    && y >= topPos + 28
                    && y < topPos + 47) {
                dragging = true;
                return true;
            }
            return super.mouseClicked(x, y, button);
        }

        @Override
        public boolean mouseReleased(double x, double y, int button) {
            if (dragging) {
                dragging = false;
                if (x >= leftPos + 202
                        && x <= leftPos + 306
                        && y >= topPos + 48
                        && y <= topPos + 138
                        && strokes.size() < 3) {
                    int id =
                            com.mpp.stellaeomphalos.client.codex.ClientKnowledgeCache.signId(
                                    signs.get(selected));
                    boolean duplicate = false;
                    for (var stroke : strokes) if (stroke.signId() == id) duplicate = true;
                    if (!duplicate && id >= 0)
                        strokes.add(
                                new com.mpp
                                        .stellaeomphalos
                                        .network
                                        .toServer
                                        .PktImprintEngrave
                                        .Stroke(
                                        id,
                                        Math.min(30, (int) ((x - leftPos - 202) * 31 / 104)),
                                        Math.min(30, (int) ((y - topPos - 48) * 31 / 90))));
                    submit.active = strokes.size() == 3;
                    if (submit.active) submit();
                }
                return true;
            }
            return super.mouseReleased(x, y, button);
        }

        @Override
        protected void renderBg(GuiGraphics g, float p, int x, int y) {
            super.renderBg(g, p, x, y);
            g.fill(leftPos + 8, topPos + 24, leftPos + 168, topPos + 28, 0xff080e1c);
            g.fill(
                    leftPos + 8,
                    topPos + 24,
                    leftPos + 8 + Math.min(160, menu.runTick() * 160 / 200),
                    topPos + 28,
                    0xff9bbfe9);
            g.fill(leftPos + 180, topPos + 28, leftPos + 328, topPos + 47, 0xff526181);
            g.drawCenteredString(font, selectedName(), leftPos + 254, topPos + 33, 0xffe4d6bc);
            g.fill(leftPos + 202, topPos + 48, leftPos + 307, topPos + 139, 0xff0b1121);
            for (int n = 0; n < strokes.size(); n++) {
                var stroke = strokes.get(n);
                int px = leftPos + 202 + stroke.gridX() * 104 / 31,
                        py = topPos + 48 + stroke.gridZ() * 90 / 31;
                g.fill(px - 3, py - 3, px + 4, py + 4, 0xffadcfff);
            }
            if (dragging) g.drawString(font, selectedName(), x + 6, y - 6, 0xffe4d6bc);
        }
    }
}
