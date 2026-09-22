package com.mpp.stellaeomphalos.client.codex;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.boon.BoonMirror;
import com.mpp.stellaeomphalos.constellation.boon.*;
import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.knowledge.research.*;
import com.mpp.stellaeomphalos.network.toServer.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

import java.util.*;

/** One screen, two disposable pages, one overlay. Opening an overlay leaves the viewport intact. */
public final class CodexScreen extends Screen {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("stellaeomphalos", "textures/gui/codex.png");
    private static final int TEXTURE_SIZE = 656;

    private record Hit(int x, int y, int width, int height, Runnable action, Component tooltip) {
        boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<net.minecraft.util.FormattedCharSequence> overlayLines = new ArrayList<>();
    private final CodexLookupIndex lookup = new CodexLookupIndex();
    private final String initialRoute;
    private StandardCodexPageView leftPage, rightPage;
    private boolean overview = true, unread, initialized;
    private long turnStarted;
    private int pendingTurn;
    private String overlay = "", shardId = "", branchFilter = "", language = "";
    private ResourceLocation signFocus;
    private StudyBranch focused;
    private EditBox query;
    private float uiScale = 1;
    private int left, top, scroll, branchScroll;
    private long refreshedRevision = -1, refreshedEpoch = -1;

    public CodexScreen(String route) {
        super(Component.translatable("stellaeomphalos.codex.title"));
        initialRoute = route;
        com.mpp.stellaeomphalos.client.sound.UiSounds.play("codex_open");
    }

    private static Component label(String name) {
        return Component.translatable("stellaeomphalos.codex." + name);
    }

    @Override
    protected void init() {
        uiScale =
                Math.min(
                        1F,
                        Math.min(
                                (width - 8F) / CodexLayout.WIDTH,
                                (height - 8F) / CodexLayout.HEIGHT));
        left = (int) ((width / uiScale - CodexLayout.WIDTH) / 2);
        top = (int) ((height / uiScale - CodexLayout.HEIGHT) / 2);
        String previous = query == null ? "" : query.getValue();
        query =
                new EditBox(
                        font,
                        (int) ((left + 24) * uiScale),
                        (int) ((top + 42) * uiScale),
                        (int) (320 * uiScale),
                        18,
                        label("search"));
        query.setValue(previous);
        query.setResponder(value -> scroll = 0);
        query.setVisible(overlay.equals("search"));
        addRenderableWidget(query);
        if (!initialized) {
            initialized = true;
            var route = CodexRoute.parse(initialRoute);
            if (route.isPresent()) navigate(route.get(), false);
            else ClientKnowledgeCache.NAVIGATOR.current().ifPresent(r -> navigate(r, false));
        }
        layoutPages();
        layoutOverlay();
        refresh();
    }

    public void refresh() {
        refreshedRevision = -1;
        refreshedEpoch = -1;
    }

    private GateContext context() {
        var mc = Minecraft.getInstance();
        return ClientKnowledgeCache.context(
                mc.level == null
                        ? new ResourceLocation("minecraft:overworld")
                        : mc.level.dimension().location(),
                mc.level == null ? 0 : mc.level.getGameTime());
    }

    private void refreshIndices() {
        String currentLanguage = minecraft.getLanguageManager().getSelected();
        if (refreshedRevision == ClientKnowledgeCache.record().revision()
                && refreshedEpoch == ClientKnowledgeCache.epoch()
                && language.equals(currentLanguage)) return;
        language = currentLanguage;
        refreshedRevision = ClientKnowledgeCache.record().revision();
        refreshedEpoch = ClientKnowledgeCache.epoch();
        ClientKnowledgeCache.SEARCH.build(
                KnowledgeCatalog.NODES,
                ClientKnowledgeCache.pages(),
                context(),
                I18n::get,
                id -> new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName().getString());
        lookup.clear();
        for (var node : KnowledgeCatalog.NODES.all())
            lookup.register(
                    new ItemStack(BuiltInRegistries.ITEM.get(node.icon())),
                    false,
                    new CodexRoute(node.id(), 0));
        if (!overview) ClientKnowledgeCache.NAVIGATOR.current().ifPresent(r -> buildViews(r));
        layoutPages();
        layoutOverlay();
    }

    private void layoutPages() {
        if (leftPage != null) leftPage.layout(CodexLayout.PAGE_WIDTH, CodexLayout.PAGE_HEIGHT);
        if (rightPage != null) rightPage.layout(CodexLayout.PAGE_WIDTH, CodexLayout.PAGE_HEIGHT);
    }

    private boolean readable(CodexRoute route) {
        return KnowledgeCatalog.NODES
                .find(route.node())
                .filter(
                        n ->
                                route.page() < n.pages().size()
                                        && n.visibility(context()).level().readable())
                .isPresent();
    }

    public boolean navigate(CodexRoute route, boolean history) {
        if (history && turnStarted == 0) com.mpp.stellaeomphalos.client.sound.UiSounds.play("codex_page_turn");
        if (!readable(route)) return false;
        if (history) ClientKnowledgeCache.NAVIGATOR.push(route);
        else ClientKnowledgeCache.NAVIGATOR.replace(route);
        overview = false;
        overlay = "";
        if (query != null) query.setVisible(false);
        buildViews(route);
        layoutPages();
        if (minecraft != null && minecraft.player != null)
            OmphalosClient.sendDependent(new PktCodexRead(route.encode()));
        return true;
    }

    private void buildViews(CodexRoute route) {
        var node = KnowledgeCatalog.NODES.find(route.node()).orElseThrow();
        int offset = route.page() / 2 * 2;
        leftPage = view(node, offset);
        rightPage = view(node, offset + 1);
    }

    private StandardCodexPageView view(StudyNode node, int index) {
        if (index >= node.pages().size()) return null;
        var page = ClientKnowledgeCache.page(node.pages().get(index));
        if (page.isEmpty() || !page.get().visibleWhen().evaluate(context()).level().readable())
            return null;
        return new StandardCodexPageView(page.get());
    }

    public void shard(String id) {
        shardId = id;
        openOverlay("shard");
    }

    public void openOverlay(String type) {
        overlay = type;
        scroll = 0;
        if (query != null) query.setVisible(type.equals("search"));
        layoutOverlay();
    }

    public void closeOverlay() {
        overlay = "";
        if (query != null) query.setVisible(false);
    }

    public Optional<CodexRoute> currentRoute() {
        return ClientKnowledgeCache.NAVIGATOR.current();
    }

    public int overlayLineCount() {
        return overlayLines.size();
    }

    private void layoutOverlay() {
        overlayLines.clear();
        if (!overlay.equals("shard")) return;
        var id = ResourceLocation.tryParse(shardId);
        if (id == null) return;
        KnowledgeCatalog.SHARDS
                .find(id)
                .ifPresent(
                        s -> {
                            overlayLines.add(Component.translatable(s.nameKey()).getVisualOrderText());
                            overlayLines.add(net.minecraft.util.FormattedCharSequence.EMPTY);
                            if (I18n.exists(s.bodyKey())) overlayLines.addAll(CodexTextLayout.layout(font,Component.translatable(s.bodyKey()),267).lines());
                        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        refreshIndices();
        renderBackground(graphics);
        double mx = mouseX / uiScale - left, my = mouseY / uiScale - top;
        var pose = graphics.pose();
        pose.pushPose();
        pose.scale(uiScale, uiScale, 1);
        pose.translate(left, top, 0);
        graphics.blit(TEXTURE, 0, 0, 393, 270, 0, 0, 393, 270, TEXTURE_SIZE, TEXTURE_SIZE);
        hits.clear();
        graphics.blit(TEXTURE, 160, 0, 48, 304, 160, 289, 48, 304, TEXTURE_SIZE, TEXTURE_SIZE);
        mark(graphics, "study", false, 32, 42, mx, my);
        mark(graphics, "signs", false, 74, 42, mx, my);
        mark(graphics, "search", false, 116, 44, mx, my);
        mark(graphics, "boons", true, 32, 42, mx, my);
        mark(graphics, "lore", true, 74, 42, mx, my);
        mark(graphics, "milestones", true, 116, 44, mx, my);
        if (overview) drawOverview(graphics, mx, my);
        else {
            float scale=1;
            if (turnStarted!=0) {
                double phase=Math.min(1,(net.minecraft.Util.getMillis()-turnStarted)/320.0);
                if(phase>=.5&&pendingTurn!=0){int delta=pendingTurn;pendingTurn=0;applyTurn(delta);}
                scale=(float)Math.max(.02,Math.abs(phase*2-1));
                if(phase>=1)turnStarted=0;
            }
            pose.pushPose();pose.translate(196,0,0);pose.scale(scale,1,1);pose.translate(-196,0,0);
            drawPages(graphics,mx,my);pose.popPose();
        }
        button(graphics, 5, 249, 47, 16, label("back"), this::goBack, mx, my);
        button(
                graphics,
                54,
                249,
                47,
                16,
                label("forward"),
                () -> ClientKnowledgeCache.NAVIGATOR.forward().ifPresent(r -> navigate(r, false)),
                mx,
                my);
        button(graphics, 331, 249, 57, 16, label("close"), this::onClose, mx, my);
        if (!overview && overlay.isEmpty()) {
            button(graphics, 155, 249, 24, 16, Component.literal("<"), () -> turn(-2), mx, my);
            button(graphics, 213, 249, 24, 16, Component.literal(">"), () -> turn(2), mx, my);
        }
        if (!overlay.isEmpty()) {
            graphics.flush();
            pose.pushPose();
            pose.translate(0, 0, 400);
            drawOverlay(graphics, mx, my);
            graphics.flush();
            pose.popPose();
        }
        for (var hit : hits)
            if (hit.contains(mx, my) && !hit.tooltip().getString().isBlank()) {
                graphics.renderTooltip(font, hit.tooltip(), (int) mx, (int) my);
                break;
            }
        pose.popPose();
        super.render(graphics, mouseX, mouseY, partial);
    }

    private void drawPages(GuiGraphics g, double mx, double my) {
        long tick = OmphalosConfig.CLIENT.flag("codex.animations") ? System.nanoTime() / 50000000L : 0;
        if (leftPage != null)
            leftPage.draw(
                    new CodexDrawContext(g, CodexLayout.LEFT, CodexLayout.TOP, (int) mx, (int) my),
                    tick);
        else g.drawString(font, label("missing"), 23, 28, 0xff772222, false);
        if (rightPage != null)
            rightPage.draw(
                    new CodexDrawContext(g, CodexLayout.RIGHT, CodexLayout.TOP, (int) mx, (int) my),
                    tick);
        var hovered = hovered(mx, my);
        hovered.ifPresent(stack -> g.renderTooltip(font, stack, (int) mx, (int) my));
    }

    private void drawOverview(GuiGraphics g, double mx, double my) {
        g.fill(12, 18, 380, 241, 0xff191a31);
        g.enableScissor(
                (int) ((left + 12) * uiScale),
                (int) ((top + 18) * uiScale),
                (int) ((left + 380) * uiScale),
                (int) ((top + 241) * uiScale));
        // Original deterministic stars, shared by every zoom stage so the canvas never exposes
        // blank margins.
        for (int i = 0; i < 80; i++) {
            int sx = 15 + Math.floorMod(i * 137, 361), sy = 22 + Math.floorMod(i * 83, 213);
            g.fill(sx, sy, sx + 1, sy + 1, 0xff585a83);
        }
        var canvas = ClientKnowledgeCache.CANVAS;
        canvas.bounds(512, 512, 368, 223);
        canvas.centerStep(
                OmphalosConfig.CLIENT.decimal("codex.focusThreshold"),
                OmphalosConfig.CLIENT.decimal("codex.branchThreshold"));
        if (canvas.scale() < OmphalosConfig.CLIENT.decimal("codex.branchThreshold")
                || focused == null) {
            int index = 0;
            for (var branch : StudyBranch.values()) {
                var point = new StudyCanvas.Point((index % 3 - 1) * 90, (index / 3 * 2 - 1) * 45);
                var screen = canvas.screen(point);
                int x = 12 + (int) screen.x(), y = 18 + (int) screen.y();
                int color =
                        ClientKnowledgeCache.record().branches().contains(branch.name())
                                ? 0xffb9b1ff
                                : 0xff575c71;
                for (int radius = 24; radius >= 4; radius -= 4) {
                    int alpha =
                            Math.max(
                                    0,
                                    (int)
                                            ((1
                                                            - Math.min(
                                                                    1,
                                                                    canvas.scale()
                                                                            / OmphalosConfig.CLIENT
                                                                                    .decimal(
                                                                                            "codex.cloudFadeThreshold")))
                                                    * (28 - radius)));
                    g.fill(
                            x - radius,
                            y - radius / 2,
                            x + radius,
                            y + radius / 2,
                            (alpha << 24) | 0x9995ff);
                }
                g.fill(x - 5, y - 5, x + 5, y + 5, color);
                var title =
                        Component.translatable(
                                "codex.stellaeomphalos.branch."
                                        + branch.name().toLowerCase(Locale.ROOT));
                g.drawCenteredString(font, title, x, y + 16, color);
                if (x >= 12 && x <= 380 && y >= 18 && y <= 241)
                    hits.add(
                            new Hit(
                                    x - 18,
                                    y - 18,
                                    36,
                                    36,
                                    () -> {
                                        canvas.focus(point);
                                        focused = branch;
                                        branchScroll = 0;
                                        canvas.doubleClick(
                                                branch.name(), System.nanoTime() / 1000000L, point);
                                    },
                                    title));
                index++;
            }
            g.drawString(font, label("zoom"), 17, 227, 0xffaaa5ba, false);
        } else {
            double localScale = Math.min(1.2, .1 + (canvas.scale() - 6) * .275);
            var nodes =
                    KnowledgeCatalog.NODES.all().stream()
                            .filter(n -> n.branch() == focused)
                            .toList();
            var positions = new HashMap<ResourceLocation, int[]>();
            for (var node : nodes)
                positions.put(
                        node.id(),
                        new int[] {
                            35 + (int) (node.x() * 1.5 * localScale),
                            38 + (int) (node.y() * 1.5 * localScale) - branchScroll
                        });
            for (var node : nodes) {
                var p = positions.get(node.id());
                if (p[1] < 25 || p[1] > 204) continue;
                for (var from : node.prerequisites()) {
                    var q = positions.get(from);
                    if (q != null && q[1] >= 25 && q[1] <= 204)
                        flowLine(g, p[0] + 8, p[1] + 8, q[0] + 8, q[1] + 8);
                }
            }
            for (var node : nodes) {
                var verdict = node.visibility(context());
                if (verdict.level() == GateLevel.HIDDEN) continue;
                var p = positions.get(node.id());
                if (p[1] < 25 || p[1] > 204) continue;
                g.pose().pushPose();
                g.pose().translate(p[0], p[1], 0);
                g.pose().scale((float) localScale, (float) localScale, 1);
                if (verdict.level().readable())
                    g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(node.icon())), 0, 0);
                else g.fill(0, 0, 16, 16, 0xff56536b);
                g.pose().popPose();
                Component title =
                        ClientKnowledgeCache.page(node.pages().get(0))
                                .map(page -> Component.translatable(page.title()))
                                .orElse(Component.empty());
                if (StudyCanvas.clickable(
                        localScale, OmphalosConfig.CLIENT.decimal("codex.nodeClickThreshold")))
                    hits.add(
                            new Hit(
                                    p[0] - 2,
                                    p[1] - 2,
                                    (int) (16 * localScale) + 4,
                                    (int) (16 * localScale) + 4,
                                    () -> navigate(new CodexRoute(node.id(), 0), true),
                                    verdict.level().readable() ? title : label("locked")));
            }
            g.drawString(
                    font,
                    Component.translatable(
                            "codex.stellaeomphalos.branch."
                                    + focused.name().toLowerCase(Locale.ROOT)),
                    18,
                    225,
                    0xffc1badb,
                    false);
        }
        g.disableScissor();
    }

    private void flowLine(GuiGraphics g, int ax, int ay, int bx, int by) {
        int length = Math.max(1, (int) Math.hypot(bx - ax, by - ay));
        int head = (int) Math.floorMod(System.nanoTime() / 50000000L, length);
        for (int i = 0; i <= length; i++) {
            int x = ax + (bx - ax) * i / length, y = ay + (by - ay) * i / length;
            int glow = Math.max(0, 10 - Math.abs(i - head));
            int color =
                    0xff000000
                            | ((70 + glow * 10) << 16)
                            | ((68 + glow * 9) << 8)
                            | (104 + glow * 10);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void ribbon(String name) {
        switch (name) {
            case "study" -> {
                overview = true;
                closeOverlay();
            }
            case "signs" -> openOverlay("signs");
            case "boons" -> openOverlay("boons");
            case "lore" -> openOverlay("lore");
            case "milestones" -> {
                OmphalosClient.sendDependent(new PktKnowledgeQuery("gauges"));
                openOverlay("gauges");
            }
            default -> {
                branchFilter = "";
                openOverlay("search");
            }
        }
    }

    private void drawOverlay(GuiGraphics g, double mx, double my) {
        // Drop only the hit targets covered by the overlay; shell buttons and ribbons stay live.
        hits.removeIf(
                hit ->
                        hit.x() < 379
                                && hit.x() + hit.width() > 14
                                && hit.y() < 239
                                && hit.y() + hit.height() > 20);
        g.fill(14, 20, 379, 239, 0xffeee0bb);
        button(g, 330, 22, 46, 16, label("close"), this::closeOverlay, mx, my);
        if (overlay.equals("search")) {
            button(
                    g,
                    20,
                    66,
                    110,
                    16,
                    unread ? label("unread") : label("all"),
                    () -> {
                        unread = !unread;
                        scroll = 0;
                    },
                    mx,
                    my);
            button(
                    g,
                    135,
                    66,
                    190,
                    16,
                    branchFilter.isEmpty() ? label("all") : Component.literal(branchFilter),
                    () -> {
                        if (branchFilter.isEmpty()) branchFilter = StudyBranch.DISCOVERY.name();
                        else {
                            int next = StudyBranch.valueOf(branchFilter).ordinal() + 1;
                            branchFilter =
                                    next >= StudyBranch.values().length
                                            ? ""
                                            : StudyBranch.values()[next].name();
                        }
                        scroll = 0;
                    },
                    mx,
                    my);
            var results =
                    ClientKnowledgeCache.SEARCH.search(
                            query.getValue(),
                            branchFilter,
                            unread,
                            ClientKnowledgeCache.record().codexSeen());
            int row = 0;
            for (var result : results.stream().skip(scroll).limit(13).toList()) {
                int y = 86 + row++ * 11;
                g.drawString(
                        font,
                        font.plainSubstrByWidth(result.title(), 340),
                        22,
                        y,
                        0xff463351,
                        false);
                hits.add(
                        new Hit(
                                20,
                                y,
                                335,
                                11,
                                () -> navigate(result.route(), true),
                                Component.empty()));
            }
        } else if (overlay.equals("lore")) {
            g.drawString(font, label("lore"), 24, 26, 0xff463351, false);
            int row = 0;
            for (var id :
                    ClientKnowledgeCache.record().unlockedShards().stream()
                            .sorted()
                            .skip(scroll)
                            .limit(17)
                            .toList()) {
                var shard = KnowledgeCatalog.SHARDS.find(id);
                if (shard.isEmpty()) continue;
                int y = 48 + row++ * 10;
                g.drawString(
                        font,
                        Component.translatable(shard.get().nameKey()),
                        23,
                        y,
                        0xff463351,
                        false);
                hits.add(new Hit(20, y, 335, 10, () -> shard(id.toString()), Component.empty()));
            }
        } else if (overlay.equals("shard")) {
            g.blit(TEXTURE, 22, 20, 48, 156, 160, 289, 48, 156, TEXTURE_SIZE, TEXTURE_SIZE);
            g.blit(TEXTURE, 22, 176, 48, 63, 160, 530, 48, 63, TEXTURE_SIZE, TEXTURE_SIZE);
            int row = 0;
            for (var line : overlayLines.stream().skip(scroll).limit(18).toList())
                g.drawString(font, line, 80, 45 + row++ * 10, 0xff463351, false);
        } else if (overlay.equals("gauges")) {
            g.drawString(font, label("gauges"), 24, 26, 0xff463351, false);
            var list =
                    ClientKnowledgeCache.gauges().getList("Readings", Tag.TAG_COMPOUND).stream()
                            .map(t -> (CompoundTag) t)
                            .sorted(Comparator.comparing(t -> I18n.get(t.getString("Name"))))
                            .skip(scroll)
                            .limit(16)
                            .toList();
            int row = 0;
            for (var t : list) {
                int y = 45 + row++ * 11;
                g.drawString(
                        font,
                        I18n.get(t.getString("Name"))
                                + ": "
                                + String.format(Locale.ROOT, "%.3f", t.getDouble("Value")),
                        23,
                        y,
                        0xff463351,
                        false);
                var detail =
                        Component.literal(
                                String.format(
                                        Locale.ROOT,
                                        "%.4f + %.4f; x(1 + %.4f); x%.4f",
                                        t.getDouble("Base"),
                                        t.getDouble("Addition"),
                                        t.getDouble("Multiply"),
                                        t.getDouble("Stacking")));
                hits.add(new Hit(20, y, 335, 11, () -> {}, detail));
            }
        } else if (overlay.equals("signs")) {
            g.drawString(font, label("signs"), 24, 26, 0xff463351, false);
            var ids = new TreeSet<ResourceLocation>(ClientKnowledgeCache.record().knownSigns());
            ids.addAll(ClientKnowledgeCache.record().seenSigns());
            int cell = 0;
            for (var id : ids.stream().skip(scroll).limit(8).toList()) {
                var sign = com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byId(id);
                if (sign == null) continue;
                int x = 20 + (cell % 4) * 88, y = 42 + (cell / 4) * 94;
                cell++;
                g.fill(x + 1, y + 1, x + 83, y + 79, 0xffe2cfa2);
                int extent = 66;
                for (var line : sign.lines())
                    com.mpp.stellaeomphalos.client.screen.CelestialScreen.line(
                            g,
                            x + 9 + line.a().x() * extent / 31,
                            y + 6 + line.a().y() * extent / 31,
                            x + 9 + line.b().x() * extent / 31,
                            y + 6 + line.b().y() * extent / 31,
                            0xff8a7ab8);
                for (var point : sign.stars()) {
                    int sx = x + 9 + point.x() * extent / 31, sy = y + 6 + point.y() * extent / 31;
                    g.fill(sx - 1, sy - 1, sx + 2, sy + 2, 0xff584a80);
                }
                g.drawCenteredString(
                        font,
                        Component.literal(
                                font.plainSubstrByWidth(sign.displayName().getString(), 80)),
                        x + 42,
                        y + 83,
                        0xff463351);
                hits.add(
                        new Hit(
                                x,
                                y,
                                84,
                                92,
                                () -> {
                                    signFocus = id;
                                    openOverlay("sign");
                                },
                                sign.displayName()));
            }
        } else if (overlay.equals("sign")) {
            var sign =
                    signFocus == null
                            ? null
                            : com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byId(
                                    signFocus);
            if (sign == null) {
                g.drawString(font, label("missing"), 24, 26, 0xff772222, false);
            } else {
                g.drawCenteredString(font, sign.displayName(), 196, 27, 0xff463351);
                int size = 168;
                int sx = 196 - size / 2, sy = 42;
                g.fill(sx - 3, sy - 3, sx + size + 3, sy + size + 3, 0xff857397);
                g.fill(sx, sy, sx + size, sy + size, 0xb8111827);
                for (var point : sign.stars()) {
                    int px = sx + point.x() * size / 31, py = sy + point.y() * size / 31;
                    g.fill(px - 1, py - 1, px + 2, py + 2, 0xffdfedff);
                }
                for (var line : sign.lines())
                    com.mpp.stellaeomphalos.client.screen.CelestialScreen.line(
                            g,
                            sx + line.a().x() * size / 31,
                            sy + line.a().y() * size / 31,
                            sx + line.b().x() * size / 31,
                            sy + line.b().y() * size / 31,
                            0xffa2bfe4);
            }
        } else if (overlay.equals("boons")) {
            var boon = BoonMirror.view();
            g.drawString(
                    font,
                    label("boons")
                            .copy()
                            .append("  Lv " + boon.level() + "  +" + boon.availablePoints()),
                    24,
                    26,
                    0xff463351,
                    false);
            var nodes = new ArrayList<BoonNodeLayout>();
            for (var sign : ClientKnowledgeCache.record().knownSigns())
                nodes.addAll(BoonTreeLayout.ofSign(sign).values());
            nodes.addAll(
                    BoonTreeLayout.ofSign(new ResourceLocation("stellaeomphalos:core")).values());
            int row = 0;
            for (var node :
                    nodes.stream()
                            .distinct()
                            .sorted(Comparator.comparing(n -> n.id().toString()))
                            .skip(scroll)
                            .limit(16)
                            .toList()) {
                int y = 45 + row++ * 11;
                String mark =
                        boon.hasNode(node.id())
                                ? (boon.isSealed(node.id()) ? "[-] " : "[+] ")
                                : "[ ] ";
                g.drawString(font, mark + node.id().getPath(), 23, y, 0xff463351, false);
                hits.add(
                        new Hit(
                                20,
                                y,
                                335,
                                11,
                                () ->
                                        OmphalosClient.sendDependent(
                                                new PktBoonAction(
                                                        boon.hasNode(node.id())
                                                                ? (boon.isSealed(node.id())
                                                                        ? "unseal"
                                                                        : "seal")
                                                                : "unlock",
                                                        node.id())),
                                Component.empty()));
            }
        }
    }

    private String activeMark() {
        if (overlay.isEmpty()) return "study";
        return switch (overlay) {
            case "lore", "shard" -> "lore";
            case "gauges" -> "milestones";
            case "boons" -> "boons";
            case "signs", "sign" -> "signs";
            default -> "search";
        };
    }

    private void mark(GuiGraphics g, String name, boolean leftSide, int y, int h, double mx, double my) {
        if (name.equals(activeMark())) return;
        var hit =
                new Hit(
                        leftSide ? -17 : 386, y + 16, leftSide ? 25 : 23, 22,
                        () -> ribbon(name), label(name));
        hits.add(hit);
        boolean stretched = hit.contains(mx, my);
        int w = stretched ? 66 : 34;
        int u = leftSide ? (stretched ? 542 : 622) : (stretched ? 448 : 400);
        int x = leftSide ? (stretched ? -49 : -17) : 376;
        g.blit(TEXTURE, x, y, w, h, u, y, w, h, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    private void button(
            GuiGraphics g,
            int x,
            int y,
            int w,
            int h,
            Component text,
            Runnable action,
            double mx,
            double my) {
        var hit = new Hit(x, y, w, h, action, Component.empty());
        hits.add(hit);
        g.fill(x, y, x + w, y + h, hit.contains(mx, my) ? 0xff9580b0 : 0xff746380);
        float textScale = Math.min(1F, (w - 4F) / Math.max(1, font.width(text)));
        g.pose().pushPose();
        g.pose().translate(x + w / 2F, y + (h - 8 * textScale) / 2F, 0);
        g.pose().scale(textScale, textScale, 1);
        g.drawCenteredString(font, text, 0, 0, 0xfff9ecd5);
        g.pose().popPose();
    }

    @Override public void onClose() {
        com.mpp.stellaeomphalos.client.sound.UiSounds.play("codex_close");super.onClose();
    }
    private void goBack() {
        if (overlay.equals("sign")) {
            openOverlay("signs");
            return;
        }
        if (!overlay.isEmpty()) {
            closeOverlay();
            return;
        }
        if (overview) return;
        if (ClientKnowledgeCache.NAVIGATOR.historySize() == 0) {
            overview = true;
            leftPage = null;
            rightPage = null;
            return;
        }
        ClientKnowledgeCache.NAVIGATOR.back().ifPresent(r -> navigate(r, false));
    }
    private void turn(int delta) {
        if(turnStarted!=0)return;
        if(OmphalosConfig.CLIENT.flag("codex.animations")){pendingTurn=delta;turnStarted=net.minecraft.Util.getMillis();com.mpp.stellaeomphalos.client.sound.UiSounds.play("codex_page_turn");}
        else applyTurn(delta);
    }
    private void applyTurn(int delta) {
        ClientKnowledgeCache.NAVIGATOR
                .current()
                .ifPresent(
                        r -> {
                            var node = KnowledgeCatalog.NODES.find(r.node()).orElseThrow();
                            int next = r.page() / 2 * 2 + delta;
                            if (next >= 0 && next < node.pages().size())
                                navigate(new CodexRoute(r.node(), next), true);
                        });
    }

    private Optional<ItemStack> hovered(double x, double y) {
        if (leftPage != null && x < CodexLayout.RIGHT)
            return leftPage.hitTest(x - CodexLayout.LEFT, y - CodexLayout.TOP);
        return rightPage == null
                ? Optional.empty()
                : rightPage.hitTest(x - CodexLayout.RIGHT, y - CodexLayout.TOP);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (overlay.equals("search") && query.mouseClicked(x, y, button)) return true;
        double mx = x / uiScale - left, my = y / uiScale - top;
        for (var hit : List.copyOf(hits))
            if (hit.contains(mx, my)) {
                hit.action().run();
                return true;
            }
        if (!overview && overlay.isEmpty()) {
            if (my >= CodexLayout.TOP + 200 && my <= CodexLayout.TOP + 218) {
                var page = mx >= CodexLayout.RIGHT ? rightPage : leftPage;
                if (page != null && page.previewTarget().isPresent()) {
                    OmphalosClient.sendDependent(new PktCodexPreview(page.previewTarget().get()));
                    return true;
                }
            }
            var item = hovered(mx, my);
            if (item.isPresent()) {
                if (hasControlDown()
                        && com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                                minecraft.getWindow().getWindow(), 292)) {
                    ClientKnowledgeCache.NAVIGATOR
                            .current()
                            .flatMap(route -> KnowledgeCatalog.NODES.find(route.node()))
                            .ifPresent(
                                    node -> {
                                        for (var pageId : node.pages())
                                            ClientKnowledgeCache.page(pageId)
                                                    .filter(
                                                            page ->
                                                                    page.kind() == PageKind.RECIPE
                                                                            || page.kind()
                                                                                    == PageKind
                                                                                            .RECIPE_ALTAR
                                                                            || page.kind()
                                                                                    == PageKind
                                                                                            .RECIPE_LIGHT)
                                                    .ifPresent(
                                                            page ->
                                                                    minecraft.keyboardHandler
                                                                            .setClipboard(
                                                                                    page
                                                                                            .reference()));
                                    });
                    return true;
                }
                lookup.find(item.get(), KnowledgeCatalog.NODES, context())
                        .ifPresent(route -> navigate(route, true));
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!overlay.isEmpty()) return super.mouseDragged(x, y, button, dx, dy);
        if (overview) {
            if (focused != null
                    && ClientKnowledgeCache.CANVAS.scale()
                            >= OmphalosConfig.CLIENT.decimal("codex.branchThreshold"))
                panBranch(-dy / uiScale);
            else ClientKnowledgeCache.CANVAS.pan(dx / uiScale, dy / uiScale);
            return true;
        }
        return x / uiScale - left >= CodexLayout.RIGHT && rightPage != null
                ? rightPage.drag(dx / uiScale)
                : leftPage != null && leftPage.drag(dx / uiScale);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (!overlay.isEmpty()) {
            scroll = Math.max(0, scroll + (delta < 0 ? 3 : -3));
            return true;
        }
        if (overview) {
            if (hasShiftDown() && focused != null) {
                panBranch(delta < 0 ? 24 : -24);
            } else
                ClientKnowledgeCache.CANVAS.zoom(
                        delta * .5, OmphalosConfig.CLIENT.decimal("codex.focusThreshold"));
            return true;
        }
        return x / uiScale - left >= CodexLayout.RIGHT && rightPage != null
                ? rightPage.scroll(delta)
                : leftPage != null && leftPage.scroll(delta);
    }

    private void panBranch(double pixels) {
        double scale = Math.min(1.2, .1 + (ClientKnowledgeCache.CANVAS.scale() - 6) * .275);
        int extent =
                KnowledgeCatalog.NODES.all().stream()
                        .filter(n -> n.branch() == focused)
                        .mapToInt(n -> n.y())
                        .max()
                        .orElse(0);
        int max = Math.max(0, (int) (extent * 1.5 * scale) - 145);
        branchScroll = Math.max(0, Math.min(max, branchScroll + (int) pixels));
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 256 && !overlay.isEmpty()) {
            goBack();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
