package com.mpp.stellaeomphalos.client.structure;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.structure.preview.PreviewSession;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.*;

/** Bounded overlay: one line buffer, distance/frustum culling and zero world mutations. */
public final class StructureMirror {
    private static final Map<ResourceLocation, PreviewSession> PREVIEWS = new LinkedHashMap<>();

    private StructureMirror() {}

    public static void attach() {
        var h = OmphalosClient.handlers();
        h.register(
                PreviewStartPayload.class,
                (mc, p) -> {
                    if (PREVIEWS.size() >= 10) PREVIEWS.remove(PREVIEWS.keySet().iterator().next());
                    PREVIEWS.put(
                            p.blueprintId(),
                            new PreviewSession(
                                    p.blueprintId(), p.origin(), p.visibilityToken(), p.ttl()));
                });
        h.register(
                PreviewDiffPayload.class,
                (mc, p) -> {
                    var s = PREVIEWS.get(p.blueprintId());
                    if (s != null && s.origin().equals(p.origin()))
                        s.update(p.visibilityToken(), p.reset(), p.relatives(), p.states());
                });
        h.register(PreviewEndPayload.class, (mc, p) -> PREVIEWS.remove(p.blueprintId()));
        h.register(AstrolabeFixPayload.class, (mc, p) -> {});
        h.register(
                SpringInfoPayload.class,
                (mc, p) -> {
                    if (mc.player != null)
                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "stellaeomphalos.spring.info", p.present(), p.fillBand()),
                                true);
                });
        h.register(StructureStatePayload.class, (mc, p) -> {});
        h.register(RiteStatePayload.class, (mc, p) -> {});
        h.register(RiteOutputPayload.class, (mc, p) -> {});
        h.register(RetrogenStatusPayload.class, (mc, p) -> {});
        h.register(
                StarfallNoticePayload.class,
                (mc, p) -> {
                    if (mc.level != null && mc.level.dimension().location().equals(p.dimension()))
                        for (int i = 0; i < 24; i++)
                            mc.level.addParticle(
                                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                                    p.x() + i * 0.8,
                                    mc.player.getY() + 60,
                                    p.z() + i * 0.2,
                                    0,
                                    -0.02,
                                    0);
                });
        ClientSessionCleaner.register("structure_previews", PREVIEWS::clear);
        MinecraftForge.EVENT_BUS.addListener(StructureMirror::tick);
        MinecraftForge.EVENT_BUS.addListener(StructureMirror::render);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) PREVIEWS.values().removeIf(s -> !s.tick());
    }

    private static void render(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var buffer = mc.renderBuffers().bufferSource();
        var vertices = buffer.getBuffer(RenderType.lines());
        var camera = e.getCamera().getPosition();
        var pose = e.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        int count = 0;
        for (var session : PREVIEWS.values()) {
            if (net.minecraft.world.phys.Vec3.atCenterOf(session.origin()).distanceToSqr(camera)
                    > 48 * 48) continue;
            for (long packed : session.cells().keySet()) {
                if (++count > 4096) break;
                var box = new AABB(session.origin().offset(BlockPos.of(packed)));
                if (e.getFrustum().isVisible(box))
                    LevelRenderer.renderLineBox(pose, vertices, box, 0.4F, 0.75F, 1, 0.5F);
            }
        }
        pose.popPose();
        buffer.endBatch(RenderType.lines());
    }
}
