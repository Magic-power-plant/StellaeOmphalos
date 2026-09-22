package com.mpp.stellaeomphalos.client.codex;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.client.charge.ChargeMirror;
import com.mpp.stellaeomphalos.client.structure.StructureMirror;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiEvent;

/** Small, bounded displays consume the server's public projections and state notifications. */
public final class GameplayHud {
    private GameplayHud() {}
    public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        var g = event.getGuiGraphics();
        int y = 8;
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
            for (var line : StructureMirror.status(hit.getBlockPos())) {
                g.drawString(mc.font, line, 8, y, 0xE2EDFF); y += 11;
            }
            if (mc.level.getBlockEntity(hit.getBlockPos()) instanceof com.mpp.stellaeomphalos.content.blockentity.rite.RitePedestalBlockEntity be) {
                g.drawString(mc.font, Component.translatable("stellaeomphalos.rite.hold_mode",
                        Component.translatable("stellaeomphalos.rite.hold." + be.holdMode().name().toLowerCase(java.util.Locale.ROOT))), 8, y, 0xE2EDFF);
                y += 11;
                g.drawString(mc.font, Component.translatable("stellaeomphalos.rite.progress_ticks",
                        Component.translatable("stellaeomphalos.rite.state." + be.displayedState().name().toLowerCase(java.util.Locale.ROOT)),
                        be.displayedProgress()), 8, y, 0xE2EDFF); y += 11;
            }
        }
        if (OmphalosConfig.CLIENT.flag("render.hudChargeBars")) {
            com.mpp.stellaeomphalos.client.hud.LumenBarHud.render(g);
        }
        var mantle = ClientKnowledgeCache.mantle(mc.player.getUUID());
        if (mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(
                com.mpp.stellaeomphalos.content.item.PartSixItems.MANTLE.get())
                && mantle.contains("Stacks")) {
            g.drawString(mc.font, Component.translatable("stellaeomphalos.hud.mantle", mantle.getInt("Stacks")), 8, y, 0xBBDFFF);
            y += 11;
        }
        if (mc.player.getMainHandItem().is(com.mpp.stellaeomphalos.content.item.PartSixItems.SKY_RESONATOR.get())) {
            int rows = 0;
            for (var entry : ClientKnowledgeCache.projections().getList("Entries", Tag.TAG_COMPOUND)) {
                if (rows++ == 8) break;
                var p = (net.minecraft.nbt.CompoundTag) entry;
                g.drawString(mc.font, Component.translatable("stellaeomphalos.hud.reader",
                        Component.translatable("stellaeomphalos.hud.direction." + (p.getInt("Sectors") == 16 ? "fine." : "") + p.getInt("Direction")),
                        Component.translatable("stellaeomphalos.hud.distance." + p.getString("Distance").toLowerCase(java.util.Locale.ROOT))), 8, y, 0xBBDFFF);
                y += 11;
            }
        }
        float fade = ClientKnowledgeCache.hudFade();
        if (fade > .03F) g.drawCenteredString(mc.font, ClientKnowledgeCache.notice(),
                g.guiWidth() / 2, g.guiHeight() / 2 - 45, ((int) (fade * 255) << 24) | 0xD9ECFF);
    }
}
