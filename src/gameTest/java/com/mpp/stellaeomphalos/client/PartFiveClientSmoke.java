package com.mpp.stellaeomphalos.client;

import com.mpp.stellaeomphalos.client.codex.*;
import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.network.toClient.*;

import net.minecraft.client.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Real client rendering, resizing and navigation; records are deterministic test snapshots. */
public final class PartFiveClientSmoke {
    private static StudyCanvas.Point center;
    private static CodexRoute route;
    private static int lineCount;

    private PartFiveClientSmoke() {}

    public static void tick(Minecraft mc, int tick) {
        if (tick == 50) {
            var fluid = com.mpp.stellaeomphalos.lumen.fluid.MoltenLumenContent.TYPE.get();
            var extension = net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
            if (extension.getStillTexture() == null || extension.getFlowingTexture() == null
                    || !extension.getStillTexture().equals(new ResourceLocation("minecraft", "block/water_still")))
                throw new AssertionError("Fluid render properties were not installed during client setup");

            var record = new CompoundTag();
            record.putString("Tier", "BRILLIANCE");
            var branches = new ListTag();
            for (var branch : com.mpp.stellaeomphalos.knowledge.research.StudyBranch.values())
                branches.add(StringTag.valueOf(branch.name()));
            record.put("Branches", branches);
            ClientKnowledgeCache.apply(new PktStarRecord(500, 1, record));
            var definitions = new CompoundTag();
            KnowledgeCatalog.PAGES
                    .defaults()
                    .forEach(
                            (id, page) ->
                                    definitions.putString(id.toString(), page.json().toString()));
            var blueprints = new CompoundTag();
            var cells = new ListTag();
            for (int x = -2; x <= 2; x++)
                for (int z = -2; z <= 2; z++) {
                    var cell = new CompoundTag();
                    cell.putInt("X", x);
                    cell.putInt("Y", 0);
                    cell.putInt("Z", z);
                    cell.putString("Block", "minecraft:stone_bricks");
                    cells.add(cell);
                }
            blueprints.put("stellaeomphalos:pattern_altar_t2", cells);
            definitions.put("Blueprints", blueprints);
            OmphalosClient.handlers().dispatch(mc, new PktCodexDefinitions(1, definitions));
            mc.setScreen(new CodexScreen(""));
        }
        if (tick == 58) screenshot(mc, "part5-overview.png");
        if (tick == 60 && mc.screen instanceof CodexScreen screen) {
            float scale =
                    Math.min(
                            1F,
                            Math.min(
                                    (screen.width - 8F) / CodexLayout.WIDTH,
                                    (screen.height - 8F) / CodexLayout.HEIGHT));
            int left = (int) ((screen.width / scale - CodexLayout.BOOK_WIDTH) / 2),
                    top = (int) ((screen.height / scale - CodexLayout.HEIGHT) / 2);
            // Two actual mouse events focus the first branch through the screen's hit-test path.
            screen.mouseClicked((left + 106) * scale, (top + 84) * scale, 0);
            screen.mouseClicked((left + 106) * scale, (top + 84) * scale, 0);
            if (ClientKnowledgeCache.CANVAS.scale() != 9.9)
                throw new AssertionError("Double-click did not focus branch");
            ClientKnowledgeCache.CANVAS.zoom(-5.9, 4);
        }
        if (tick == 68) screenshot(mc, "part5-zoom4.png");
        if (tick == 72) ClientKnowledgeCache.CANVAS.zoom(2, 4);
        if (tick == 78) screenshot(mc, "part5-zoom6.png");
        if (tick == 82) ClientKnowledgeCache.CANVAS.zoom(4, 4);
        if (tick == 88) screenshot(mc, "part5-zoom10.png");
        if (tick == 95) {
            route =
                    new CodexRoute(
                            new ResourceLocation("stellaeomphalos:structure/pattern_altar_t2"), 0);
            if (!(mc.screen instanceof CodexScreen screen) || !screen.navigate(route, true))
                throw new AssertionError("Codex route did not open");
        }
        if (tick == 130) screenshot(mc, "part5-structure.png");
        if (tick == 135 && mc.screen instanceof CodexScreen screen) {
            center = ClientKnowledgeCache.CANVAS.center();
            screen.shard(KnowledgeCatalog.SHARDS.all().get(0).id().toString());
            lineCount = screen.overlayLineCount();
            for (int i = 0; i < 3; i++)
                screen.resize(
                        mc,
                        mc.getWindow().getGuiScaledWidth() - i * 10,
                        mc.getWindow().getGuiScaledHeight() - i * 5);
            if (screen.overlayLineCount() != lineCount)
                throw new AssertionError("Overlay layout accumulated lines");
            screen.resize(
                    mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
        if (tick == 145) screenshot(mc, "part5-lore.png");
        if (tick == 150 && mc.screen instanceof CodexScreen screen) {
            screen.closeOverlay();
            if (!ClientKnowledgeCache.CANVAS.center().equals(center))
                throw new AssertionError("Overlay changed viewport");
            screen.onClose();
            mc.setScreen(new CodexScreen(""));
            if (!((CodexScreen) mc.screen).currentRoute().orElseThrow().equals(route))
                throw new AssertionError("Reading route not restored");
        }
        if (tick == 160) {
            ClientKnowledgeCache.clear();
            if (ClientKnowledgeCache.ready()
                    || ClientKnowledgeCache.hudFade() != 0
                    || ClientKnowledgeCache.NAVIGATOR.current().isPresent()
                    || ClientKnowledgeCache.CANVAS.scale() != 1)
                throw new AssertionError("Session state leaked");
            com.mojang.logging.LogUtils.getLogger()
                    .info(
                            "PART5_CLIENT_SMOKE_PASS: rendering, overlay resize, reading route,"
                                    + " session cleanup");
            mc.stop();
        }
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(
                mc.gameDirectory,
                name,
                mc.getMainRenderTarget(),
                text ->
                        com.mojang.logging.LogUtils.getLogger()
                                .info("Part-5 screenshot: {}", text.getString()));
    }
}
