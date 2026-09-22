package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.sound.LoopingMachineSound;
import com.mpp.stellaeomphalos.client.view.ViewCaptureCache;
import com.mpp.stellaeomphalos.network.toClient.PktGatewayTargets;
import com.mpp.stellaeomphalos.network.toServer.PktGatewayTravel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Focus -> server-owned 95 tick charge -> travel. Closing cancels the one-use session. */
public final class GatewayScreen extends Screen {
    private final PktGatewayTargets packet;
    private Component[] labels;
    private int page, selected = -1, charge;
    private boolean charging;
    private LoopingMachineSound sound;
    private ResourceLocation preview;

    public GatewayScreen(PktGatewayTargets packet) {
        super(Component.translatable("stellaeomphalos.visual.gateway"));
        this.packet = packet;
    }

    @Override
    protected void init() {
        labels = new Component[packet.targets().size()];
        for (int i = 0; i < labels.length; i++)
            labels[i] =
                    Component.translatable(
                            "stellaeomphalos.visual.gateway_target",
                            packet.targets().get(i).toShortString());
        for (int row = 0; row < 6; row++) {
            int index = page * 6 + row;
            if (index >= labels.length) break;
            final int target = index;
            addRenderableWidget(
                    Button.builder(labels[index], b -> focusTarget(target))
                            .bounds(width / 2 - 154, height / 2 - 94 + row * 24, 168, 20)
                            .build());
        }
        addRenderableWidget(
                Button.builder(
                                Component.literal("<"),
                                b -> {
                                    if (!charging && page > 0) {
                                        page--;
                                        rebuildWidgets();
                                    }
                                })
                        .bounds(width / 2 - 154, height / 2 + 54, 36, 20)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.literal(">"),
                                b -> {
                                    if (!charging && (page + 1) * 6 < labels.length) {
                                        page++;
                                        rebuildWidgets();
                                    }
                                })
                        .bounds(width / 2 - 112, height / 2 + 54, 36, 20)
                        .build());
        addRenderableWidget(
                Button.builder(
                                Component.translatable("stellaeomphalos.visual.gateway_charge"),
                                b -> charge())
                        .bounds(width / 2 + 24, height / 2 + 54, 130, 20)
                        .build());
        addRenderableWidget(
                Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                        .bounds(width / 2 - 50, height / 2 + 94, 100, 20)
                        .build());
    }

    public void focusTarget(int target) {
        if (charging || target < 0 || target >= packet.targets().size()) return;
        selected = target;
        preview = ViewCaptureCache.texture(packet.dimension(), packet.targets().get(target));
    }

    public void charge() {
        if (charging || selected < 0) return;
        if (!OmphalosClient.sendDependent(
                new PktGatewayTravel(packet.token(), packet.targets().get(selected), false)))
            return;
        charging = true;
        charge = 0;
        var event =
                net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                        new ResourceLocation("stellaeomphalos:gateway_charge"));
        if (event != null) {
            sound =
                    new LoopingMachineSound(
                            event, packet.origin(), () -> charging && minecraft.screen == this);
            minecraft.getSoundManager().play(sound);
        }
    }

    @Override
    public void tick() {
        if (minecraft.player == null
                || minecraft.level == null
                || !minecraft.level.dimension().location().equals(packet.dimension())
                || minecraft.player.distanceToSqr(
                                packet.origin().getX() + .5,
                                packet.origin().getY() + .5,
                                packet.origin().getZ() + .5)
                        > 36) {
            onClose();
            return;
        }
        if (charging && ++charge > 135) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.fill(width / 2 - 164, height / 2 - 110, width / 2 + 164, height / 2 + 82, 0xee121c31);
        g.drawCenteredString(font, title, width / 2, Math.max(5, height / 2 - 116), 0xffe8d7b3);
        if (preview != null)
            g.blit(preview, width / 2 + 22, height / 2 - 92, 132, 75, 0, 0, 160, 90, 160, 90);
        if (charging) {
            g.fill(width / 2 + 22, height / 2 + 32, width / 2 + 154, height / 2 + 38, 0xff25394b);
            g.fill(
                    width / 2 + 22,
                    height / 2 + 32,
                    width / 2 + 22 + Math.min(132, charge * 132 / 95),
                    height / 2 + 38,
                    0xffa1d4ff);
        }
        if (labels.length == 0)
            g.drawCenteredString(
                    font,
                    Component.translatable("stellaeomphalos.visual.gateway_empty"),
                    width / 2,
                    height / 2,
                    0xffbfceef);
        super.render(g, mx, my, partial);
    }

    @Override
    public void removed() {
        charging = false;
        if (sound != null) {
            sound.finish();
            minecraft.getSoundManager().stop(sound);
            sound = null;
        }
        if (minecraft.player != null)
            OmphalosClient.sendDependent(
                    new PktGatewayTravel(packet.token(), packet.origin(), true));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public float chargeFraction() {
        return charging ? Math.min(1, charge / 95F) : 0;
    }
}
