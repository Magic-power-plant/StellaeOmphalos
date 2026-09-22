package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.client.codex.ClientKnowledgeCache;
import com.mpp.stellaeomphalos.client.sign.SignSkyMirror;
import com.mpp.stellaeomphalos.constellation.sign.Sign;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only star atlas and drag-to-connect observation canvas. Server progress is never edited. */
public final class CelestialScreen extends Screen {
    public enum Mode {
        SIGN_LIST,
        SIGN_DETAIL,
        OBSERVATORY,
        TELESCOPE,
        HAND_TELESCOPE,
        SIGN_SCROLL,
        LORE_SCROLL
    }

    private final Screen parent;
    private final Mode mode;
    private net.minecraft.resources.ResourceLocation focus;

    public CelestialScreen focus(net.minecraft.resources.ResourceLocation id) {
        focus = id;
        return this;
    }

    private final java.util.List<Sign> signs = new java.util.ArrayList<>();
    private SignCanvas canvas;
    private int selected, left, top, size;
    private Component caption = Component.empty();
    private java.util.List<net.minecraft.resources.ResourceLocation> scrollSigns =
            java.util.List.of();
    private Component[] names = new Component[0];
    private int listPage;
    private int feedbackTicks,submittedTicks;

    public CelestialScreen sources(java.util.List<net.minecraft.resources.ResourceLocation> ids) {
        scrollSigns = java.util.List.copyOf(ids);
        return this;
    }

    private Button next;
    private net.minecraft.resources.ResourceLocation submitted;

    private boolean observing() {
        return mode == Mode.OBSERVATORY || mode == Mode.TELESCOPE || mode == Mode.HAND_TELESCOPE;
    }

    private boolean usable() {
        return minecraft.level != null
                && minecraft.player != null
                && minecraft.level.isNight()
                && !minecraft.level.isRaining()
                && (mode != Mode.HAND_TELESCOPE
                        || !signs.isEmpty()
                                && com.mpp.stellaeomphalos.constellation.sign.SignAim.aligned(
                                        SignSkyMirror.layout(minecraft.level.dimension())
                                                .get(signs.get(selected).id()),
                                        minecraft.player.getYRot(),
                                        minecraft.player.getXRot()));
    }

    private void submit() {
        if (!observing() || !usable() || signs.isEmpty()) return;
        var sign = signs.get(selected);
        if (sign.id().equals(submitted)) return;
        var proof =
                new java.util.ArrayList<
                        com.mpp.stellaeomphalos.network.toServer.PktObserveSign.Edge>();
        for (var edge : canvas.lines())
            proof.add(
                    new com.mpp.stellaeomphalos.network.toServer.PktObserveSign.Edge(
                            edge.a().x(), edge.a().y(), edge.b().x(), edge.b().y()));
        var origin =
                parent instanceof StationScreens.Station<?> station
                        ? station.getMenu().pos()
                        : minecraft.player.blockPosition();
        if (com.mpp.stellaeomphalos.client.OmphalosClient.sendDependent(
                new com.mpp.stellaeomphalos.network.toServer.PktObserveSign(
                        SignSkyMirror.session(),
                        minecraft.level.dimension().location(),
                        origin,
                        mode == Mode.HAND_TELESCOPE,
                        sign.id(),
                        proof))) {
            submitted = sign.id();
            caption = Component.translatable("stellaeomphalos.visual.waiting");
        }
    }

    public CelestialScreen(Screen parent, Mode mode) {
        super(
                Component.translatable(
                        "stellaeomphalos.visual."
                                + mode.name().toLowerCase(java.util.Locale.ROOT)));
        this.parent = parent;
        this.mode = mode;
    }

    @Override
    protected void init() {
        signs.clear();
        var mc = minecraft;
        if (mc.level != null
                && (mode == Mode.OBSERVATORY
                        || mode == Mode.TELESCOPE
                        || mode == Mode.HAND_TELESCOPE)) {
            for (var active : SignSkyMirror.activeSigns(mc.level.dimension())) {
                var definition=com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byNumber(active.numericId());
                if(definition!=null&&(mode!=Mode.HAND_TELESCOPE||com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.major(definition)))signs.add(definition);
            }
        } else {
            var ids =
                    new java.util.TreeSet<net.minecraft.resources.ResourceLocation>(
                            ClientKnowledgeCache.record().knownSigns());
            ids.addAll(ClientKnowledgeCache.record().seenSigns());
            if (mode == Mode.LORE_SCROLL) {
                ids.clear();
                ids.addAll(scrollSigns);
            }
            for (var id : ids) {
                var sign = com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byId(id);
                if (sign != null) signs.add(sign);
            }
        }
        if (focus != null) {
            var chosen = com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byId(focus);
            if (chosen != null) {
                signs.clear();
                signs.add(chosen);
            }
        }
        names = new Component[signs.size()];
        for (int i = 0; i < names.length; i++) names[i] = signs.get(i).displayName();
        size = Math.max(80, Math.min(220, height - 100));
        left = (width - size) / 2;
        top = 48;
        selected = Math.min(selected, Math.max(0, signs.size() - 1));
        select();
        next =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable("stellaeomphalos.visual.next_sign"),
                                        b -> {
                                            if (!signs.isEmpty()) {
                                                if (mode == Mode.SIGN_LIST)
                                                    listPage =
                                                            (listPage + 1)
                                                                    % Math.max(
                                                                            1,
                                                                            (signs.size() + 3) / 4);
                                                else {
                                                    selected = (selected + 1) % signs.size();
                                                    select();
                                                }
                                            }
                                        })
                                .bounds(width / 2 - 104, height - 30, 100, 20)
                                .build());
        next.active = !signs.isEmpty();
        addRenderableWidget(
                Button.builder(Component.translatable("gui.back"), b -> onClose())
                        .bounds(width / 2 + 4, height - 30, 100, 20)
                        .build());
    }

    private void select() {
        submitted = null;
        canvas =
                new SignCanvas(signs.isEmpty() ? java.util.List.of() : signs.get(selected).stars());
        caption =
                signs.isEmpty()
                        ? Component.translatable("stellaeomphalos.visual.no_signs")
                        : signs.get(selected).displayName();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (mode == Mode.SIGN_LIST
                && button == 0
                && x >= left
                && y >= top
                && x < left + size
                && y < top + size) {
            int cell = size / 2,
                    index = listPage * 4 + (int) (x - left) / cell + 2 * ((int) (y - top) / cell);
            if (index < signs.size())
                minecraft.setScreen(
                        new CelestialScreen(this, Mode.SIGN_DETAIL).focus(signs.get(index).id()));
            return true;
        }
        if (observing() && usable() && x >= left && y >= top && x < left + size && y < top + size) {
            if (button == 1) canvas.undo();
            else canvas.begin((x - left) * 31 / size, (y - top) * 31 / size, 8.0 * 31 / size);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (observing() && hasControlDown() && minecraft.player != null) {
            minecraft.player.turn(dx, dy);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (observing() && usable() && button == 0 && canvas != null) {
            canvas.release((x - left) * 31 / size, (y - top) * 31 / size, 8.0 * 31 / size);
            if (!signs.isEmpty() && canvas.matches(signs.get(selected).lines())) {
                caption = Component.translatable("stellaeomphalos.visual.pattern_matched");
                submit();submittedTicks=0;
            } else if(!signs.isEmpty()&&canvas.lines().size()>=signs.get(selected).lines().size()){
                feedbackTicks=10;canvas.clear();caption=Component.translatable("stellaeomphalos.visual.pattern_failed");
            }
        }
        return super.mouseReleased(x, y, button);
    }

    public static void line(GuiGraphics g, int x, int y, int ex, int ey, int color) {
        int n = Math.max(Math.abs(ex - x), Math.abs(ey - y));
        for (int i = 0; i <= n; i++) {
            int px = x + (ex - x) * i / Math.max(1, n), py = y + (ey - y) * i / Math.max(1, n);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        StarfieldBackdrop.render(g, width, height, mx, my);
        g.drawCenteredString(font, title, width / 2, 12, 0xe8d9b8);
        g.drawCenteredString(font, caption, width / 2, 30, 0xbddaff);
        if (mode == Mode.SIGN_LIST) {
            drawAtlas(g, mx, my);
            super.render(g, mx, my, partial);
            return;
        }
        g.fill(left - 3, top - 3, left + size + 3, top + size + 3, feedbackTicks>0?0xffca5f70:0xff857397);
        g.fill(left, top, left + size, top + size, 0xb8111827);
        if (!observing() || usable())
            for (var p : canvas.stars()) {
                int x = left + p.x() * size / 31, y = top + p.y() * size / 31;
                g.fill(x - 1, y - 1, x + 2, y + 2, 0xffdfedff);
            }
        var lines =
                mode == Mode.SIGN_DETAIL || mode == Mode.SIGN_SCROLL || mode == Mode.LORE_SCROLL
                        ? signs.isEmpty()
                                ? java.util.List
                                        .<com.mpp.stellaeomphalos.constellation.starmap.StarLine>
                                                of()
                                : signs.get(selected).lines()
                        : canvas.lines();
        for (var edge : lines)
            line(
                    g,
                    left + edge.a().x() * size / 31,
                    top + edge.a().y() * size / 31,
                    left + edge.b().x() * size / 31,
                    top + edge.b().y() * size / 31,
                    0xffa2bfe4);
        super.render(g, mx, my, partial);
    }

    private void drawAtlas(GuiGraphics g, int mx, int my) {
        int cell = size / 2;
        for (int i = 0; i < 4; i++) {
            int index = listPage * 4 + i;
            if (index >= signs.size()) break;
            int x = left + (i % 2) * cell, y = top + (i / 2) * cell;
            boolean hover = mx >= x && mx < x + cell && my >= y && my < y + cell;
            g.fill(x + 2, y + 2, x + cell - 2, y + cell - 2, hover ? 0xff283d5d : 0xff151f35);
            var sign = signs.get(index);
            int extent = cell - 24;
            for (int n = 0; n < sign.lines().size(); n++) {
                var line = sign.lines().get(n);
                line(
                        g,
                        x + 12 + line.a().x() * extent / 31,
                        y + 8 + line.a().y() * extent / 31,
                        x + 12 + line.b().x() * extent / 31,
                        y + 8 + line.b().y() * extent / 31,
                        0xffa6bbdf);
            }
            for (int n = 0; n < sign.stars().size(); n++) {
                var p = sign.stars().get(n);
                int sx = x + 12 + p.x() * extent / 31, sy = y + 8 + p.y() * extent / 31;
                g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xffd7edff);
            }
            g.drawCenteredString(font, names[index], x + cell / 2, y + cell - 12, 0xffd5c6a8);
        }
    }

    @Override
    public void tick() {
        if(feedbackTicks>0)feedbackTicks--;
        if(submitted!=null&&!ClientKnowledgeCache.record().knownSigns().contains(submitted)&&++submittedTicks>100){submitted=null;canvas.clear();caption=Component.translatable("stellaeomphalos.visual.pattern_failed");}

        if (submitted != null && ClientKnowledgeCache.record().knownSigns().contains(submitted))
            caption = Component.translatable("stellaeomphalos.visual.discovered");
        if (observing() && !usable())
            caption = Component.translatable("stellaeomphalos.visual.sky_unavailable");
        if (parent instanceof StationScreens.Station<?> station
                && (minecraft.player == null
                        || minecraft.player.containerMenu != station.getMenu()
                        || !station.getMenu().stillValid(minecraft.player))) {
            station.resumeObservation();
            minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        if (parent instanceof StationScreens.Station<?> station) station.resumeObservation();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
