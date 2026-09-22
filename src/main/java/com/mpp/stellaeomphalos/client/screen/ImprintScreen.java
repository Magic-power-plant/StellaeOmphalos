package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.codex.ClientKnowledgeCache;
import com.mpp.stellaeomphalos.client.sign.SignSkyMirror;
import com.mpp.stellaeomphalos.constellation.starmap.SignDrawn;
import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;
import com.mpp.stellaeomphalos.network.toServer.PktImprintEngrave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import java.util.*;

/** Draws only client strokes; the server validates the station and compiles all effects. */
public final class ImprintScreen extends Screen {
    private final BlockPos origin;
    private final ResourceLocation dimension;
    private final int session, slot;
    private final List<ResourceLocation> signs;
    private final List<PktImprintEngrave.Stroke> strokes = new ArrayList<>();
    private int selected;
    private Button submit;
    public ImprintScreen(BlockPos origin) {
        super(Component.translatable("stellaeomphalos.imprint.title"));
        var mc = Minecraft.getInstance();
        this.origin = origin.immutable();
        dimension = mc.level.dimension().location();
        session = SignSkyMirror.session();
        slot = mc.player.getInventory().selected;
        signs = ClientKnowledgeCache.record().knownSigns().stream()
                .filter(id -> ClientKnowledgeCache.signId(id) >= 0).sorted().toList();
    }
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getEntity().isShiftKeyDown() || !ClientKnowledgeCache.ready()
                || !net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock())
                        .toString().equals("stellaeomphalos:observatory")
                || !net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getEntity().getOffhandItem().getItem())
                        .toString().equals("stellaeomphalos:sign_chart")) return;
        Minecraft.getInstance().setScreen(new ImprintScreen(event.getPos()));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
    private Component selectedName() {
        return signs.isEmpty() ? Component.translatable("stellaeomphalos.imprint.no_signs")
                : Component.translatable("sign.stellaeomphalos." + signs.get(selected).getPath());
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(selectedName(), b -> {
            if (!signs.isEmpty()) selected = (selected + 1) % signs.size();
            b.setMessage(selectedName());
        }).bounds(width / 2 - 100, height / 2 - 110, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("stellaeomphalos.imprint.clear"), b -> {
            strokes.clear(); submit.active = false;
        }).bounds(width / 2 - 100, height / 2 + 88, 95, 20).build());
        submit = addRenderableWidget(Button.builder(Component.translatable("stellaeomphalos.imprint.engrave"), b -> {
            if (OmphalosClient.sendDependent(new PktImprintEngrave(session, dimension, origin, slot, strokes))) onClose();
        }).bounds(width / 2 + 5, height / 2 + 88, 95, 20).build());
        submit.active = !strokes.isEmpty();
    }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.level == null || !minecraft.level.dimension().location().equals(dimension)
                || minecraft.player.getInventory().selected != slot || SignSkyMirror.session() != session
                || minecraft.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin)) > 36) onClose();
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        int left = width / 2 - 80, top = height / 2 - 80;
        if (button == 0 && !signs.isEmpty() && strokes.size() < PktImprintEngrave.MAX_STROKES
                && x >= left && y >= top && x < left + 160 && y < top + 160) {
            int limit = StarPoint.GRID - SignDrawn.DRAW_SIZE;
            strokes.add(new PktImprintEngrave.Stroke(ClientKnowledgeCache.signId(signs.get(selected)),
                    Math.min(limit, (int) ((x - left) / 5)), Math.min(limit, (int) ((y - top) / 5))));
            submit.active = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, Math.max(6, height / 2 - 128), 0xFFFFFF);
        int left = width / 2 - 80, top = height / 2 - 80;
        g.fill(left, top, left + 160, top + 160, 0xFF151D35);
        for (int i = 0; i <= 32; i++) {
            g.fill(left + i * 5, top, left + i * 5 + 1, top + 160, 0xFF26344C);
            g.fill(left, top + i * 5, left + 160, top + i * 5 + 1, 0xFF26344C);
        }
        var definitions = ClientKnowledgeCache.signs();
        for (var stroke : strokes) for (var id : signs) {
            if (ClientKnowledgeCache.signId(id) != stroke.signId()) continue;
            for (var point : definitions.getList(id.toString(), net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var star = (net.minecraft.nbt.CompoundTag) point;
                int x = left + (stroke.gridX() + star.getInt("X")) * 5;
                int y = top + (stroke.gridZ() + star.getInt("Y")) * 5;
                if (x < left + 160 && y < top + 160) g.fill(x, y, x + 3, y + 3, 0xFFE2EDFF);
            }
        }
        super.render(g, mx, my, partial);
    }
    @Override public boolean isPauseScreen() { return false; }
}
