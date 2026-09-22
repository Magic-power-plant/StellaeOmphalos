package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.client.boon.BoonMirror;
import com.mpp.stellaeomphalos.client.codex.ClientKnowledgeCache;
import com.mpp.stellaeomphalos.constellation.boon.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Hit boxes and translated labels are rebuilt only on layout/progress changes. */
public final class BoonTreeScreen extends Screen {
    private record Hit(BoonNodeLayout node, int x, int y, Component label) {
        boolean contains(double px, double py) {
            return Math.abs(px - x) <= 7 && Math.abs(py - y) <= 7;
        }
    }

    private final Screen parent;
    private final java.util.List<Hit> hits = new java.util.ArrayList<>();
    private double zoom = 1, panX, panY;
    private boolean layoutDirty = true;
    private Object progress;
    private int version;
    private EditBox search;

    private record Edge(Hit from, Hit to) {}

    private final java.util.List<Edge> edges = new java.util.ArrayList<>();
    private Hit pressed;
    private double pressX, pressY;
    private boolean dragged;
    private int layouts;
    private long knowledgeRevision = -1;

    public int layoutCount() {
        return layouts;
    }

    public BoonTreeScreen(Screen parent) {
        super(Component.translatable("stellaeomphalos.codex.boons"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        String previous = search == null ? "" : search.getValue();
        search = addRenderableWidget(new EditBox(font, width / 2 - 100, 28, 200, 18, title));
        search.setValue(previous);
        search.setResponder(s -> layoutDirty = true);
        addRenderableWidget(
                Button.builder(Component.translatable("gui.back"), b -> onClose())
                        .bounds(width / 2 - 50, height - 26, 100, 20)
                        .build());
        layoutDirty = true;
        layout();
    }

    @Override
    public void tick() {
        if (progress != BoonMirror.view()
                || version != BoonMirror.treeVersion()
                || knowledgeRevision != ClientKnowledgeCache.record().revision()) {
            progress = BoonMirror.view();
            version = BoonMirror.treeVersion();
            knowledgeRevision = ClientKnowledgeCache.record().revision();
            layoutDirty = true;
        }
        if (layoutDirty) layout();
    }

    private void layout() {
        hits.clear();
        var nodes = new java.util.TreeMap<ResourceLocation, BoonNodeLayout>();
        for (var id : ClientKnowledgeCache.record().knownSigns())
            nodes.putAll(BoonTreeLayout.ofSign(id));
        nodes.putAll(BoonTreeLayout.ofSign(new ResourceLocation("stellaeomphalos:core")));
        for (var n : nodes.values()) {
            if (!search.getValue().isBlank()
                    && !n.id().getPath()
                            .contains(search.getValue().toLowerCase(java.util.Locale.ROOT)))
                continue;
            int x = (int) (width / 2 + panX + n.gridX() * 20 * zoom),
                    y = (int) (height / 2 + panY + n.gridZ() * 20 * zoom);
            hits.add(new Hit(n, x, y, Component.literal(n.id().getPath())));
        }
        edges.clear();
        {
            var byId = new java.util.HashMap<ResourceLocation, Hit>();
            for (var hit : hits) byId.put(hit.node.id(), hit);
            for (var edge : BoonMirror.edges()) {
                var a = byId.get(edge.a());
                var b = byId.get(edge.b());
                if (a != null && b != null) edges.add(new Edge(a, b));
            }
        }
        layouts++;
        layoutDirty = false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float p) {
        renderBackground(g);
        g.fill(12, 52, width - 12, height - 32, 0xff10172a);
        g.enableScissor(12, 52, width - 12, height - 32);
        for (int i = 0; i < edges.size(); i++) {
            var edge = edges.get(i);
            boolean a = BoonMirror.view().hasNode(edge.from.node.id()),
                    b = BoonMirror.view().hasNode(edge.to.node.id());
            CelestialScreen.line(
                    g,
                    edge.from.x,
                    edge.from.y,
                    edge.to.x,
                    edge.to.y,
                    a && b ? 0xffb6dfff : a || b ? 0xff777da4 : 0xff303b56);
        }
        Hit hovered = null;
        for (int i = 0; i < hits.size(); i++) {
            var h = hits.get(i);
            boolean allocated = BoonMirror.view().hasNode(h.node.id());
            int color =
                    allocated
                            ? BoonMirror.view().isSealed(h.node.id()) ? 0xffd08e8e : 0xffbaddff
                            : 0xff626a91;
            g.fill(h.x - 5, h.y - 5, h.x + 6, h.y + 6, color);
            var gem = BoonMirror.socketedIn(h.node.id());
            if (!gem.isEmpty()) g.renderFakeItem(gem, h.x - 8, h.y - 8);
            if (h.contains(mx, my) && zoom >= .5) hovered = h;
        }
        g.disableScissor();
        if (pressed != null
                && pressed.node.kind() == BoonNodeType.SOCKET.ordinal()
                && minecraft.player != null)
            g.renderFakeItem(minecraft.player.getMainHandItem(), mx - 8, my - 8);
        if (hovered != null) g.renderTooltip(font, hovered.label, mx, my);
        g.drawCenteredString(font, title, width / 2, 12, 0xe8d9b8);
        super.render(g, mx, my, p);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if (layoutDirty) layout();
        pressX = x;
        pressY = y;
        dragged = false;
        pressed = null;
        if (x > 12 && x < width - 12 && y > 52 && y < height - 32 && zoom >= .5)
            for (var h : hits)
                if (h.contains(x, y)) {
                    pressed = h;
                    return true;
                }
        return false;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        Hit from = pressed;
        pressed = null;
        if (from != null && x > 12 && x < width - 12 && y > 52 && y < height - 32) {
            for (var target : hits)
                if (target.contains(x, y)) {
                    var view = BoonMirror.view();
                    String action;
                    if (target.node.kind() == BoonNodeType.SOCKET.ordinal()
                            && view.hasNode(target.node.id()))
                        action = button == 1 ? "unsocket" : "socket";
                    else if (!dragged && target == from)
                        action =
                                view.hasNode(target.node.id())
                                        ? (view.isSealed(target.node.id()) ? "unseal" : "seal")
                                        : "unlock";
                    else return true;
                    com.mpp.stellaeomphalos.client.OmphalosClient.sendDependent(
                            new com.mpp.stellaeomphalos.network.toServer.PktBoonAction(
                                    action, target.node.id()));
                    return true;
                }
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button != 0 || y <= 52 || y >= height - 32)
            return super.mouseDragged(x, y, button, dx, dy);
        if (Math.hypot(x - pressX, y - pressY) > 3) dragged = true;
        if (pressed != null) return true;
        panX = Math.max(-2000, Math.min(2000, panX + dx));
        panY = Math.max(-2000, Math.min(2000, panY + dy));
        layoutDirty = true;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        double next = Math.max(.35, Math.min(2, zoom * Math.pow(1.08, delta)));
        panX = x - width / 2 - (x - width / 2 - panX) * next / zoom;
        panY = y - height / 2 - (y - height / 2 - panY) * next / zoom;
        zoom = next;
        layoutDirty = true;
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public int hitCount() {
        return hits.size();
    }
}
