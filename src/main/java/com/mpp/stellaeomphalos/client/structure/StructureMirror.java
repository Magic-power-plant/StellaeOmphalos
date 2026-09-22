package com.mpp.stellaeomphalos.client.structure;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.structure.preview.PreviewSession;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.*;

/** Bounded overlay: one line buffer, distance/frustum culling and zero world mutations. */
public final class StructureMirror {
    private static final Map<ResourceLocation, PreviewSession> PREVIEWS = new LinkedHashMap<>();
    private static final Map<BlockPos, StructureStatePayload> STRUCTURES = new LinkedHashMap<>();
    private static final Map<BlockPos, RiteStatePayload> RITES = new LinkedHashMap<>();

    private static <T> void remember(Map<BlockPos, T> map, BlockPos pos, T value) {
        if (map.size() >= 256 && !map.containsKey(pos)) map.remove(map.keySet().iterator().next());
        map.put(pos.immutable(), value);
    }

    public static java.util.List<net.minecraft.network.chat.Component> status(BlockPos pos) {
        var result = new java.util.ArrayList<net.minecraft.network.chat.Component>();
        var structure = STRUCTURES.get(pos);
        if (structure != null)
            result.add(
                    net.minecraft.network.chat.Component.translatable(
                            "stellaeomphalos.structure.status",
                            structure.formedPercent(),
                            structure.degradations()));
        var rite = RITES.get(pos);
        if (rite != null)
            result.add(
                    net.minecraft.network.chat.Component.translatable(
                            "stellaeomphalos.rite.status",
                            net.minecraft.network.chat.Component.translatable(
                                    "stellaeomphalos.rite.state."
                                            + com.mpp.stellaeomphalos.ritual.rite.RiteState
                                                    .values()[rite.state()]
                                                    .name()
                                                    .toLowerCase(java.util.Locale.ROOT)),
                            rite.progressPercent()));
        return java.util.List.copyOf(result);
    }

    private static void clear() {
        PREVIEWS.clear();
        STRUCTURES.clear();
        RITES.clear();
    }

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
                    var session = PREVIEWS.get(p.blueprintId());
                    com.mpp.stellaeomphalos.client.event.ClientRenderEvents.EFFECTS.spawn(
                            new com.mpp.stellaeomphalos.client.effect.GhostStructureTrack(
                                    session,
                                    p.ttl(),
                                    () -> PREVIEWS.get(p.blueprintId()) == session));
                });
        h.register(
                PreviewDiffPayload.class,
                (mc, p) -> {
                    var s = PREVIEWS.get(p.blueprintId());
                    if (s != null && s.origin().equals(p.origin()))
                        s.update(p.visibilityToken(), p.reset(), p.relatives(), p.states());
                });
        h.register(PreviewEndPayload.class, (mc, p) -> PREVIEWS.remove(p.blueprintId()));
        h.register(
                AstrolabeFixPayload.class,
                (mc, p) -> {
                    com.mpp.stellaeomphalos.client.screen.AstrolabeTargetCache.accept(p);
                    if (p.deniedReason() == 0)
                        com.mpp.stellaeomphalos.client.sound.UiSounds.play("astrolabe_found");
                    if (mc.player == null) return;
                    var message =
                            p.deniedReason() != 0
                                    ? net.minecraft.network.chat.Component.translatable(
                                            "stellaeomphalos.astrolabe.unavailable")
                                    : p.exact().isPresent()
                                            ? net.minecraft.network.chat.Component.translatable(
                                                    "stellaeomphalos.astrolabe.exact",
                                                    p.targetId().toString(),
                                                    p.exact().get().toShortString())
                                            : net.minecraft.network.chat.Component.translatable(
                                                    "stellaeomphalos.astrolabe.fix",
                                                    p.targetId().toString(),
                                                    p.bearingQ() / 10.0,
                                                    p.distanceBand());
                    mc.player.displayClientMessage(message, true);
                });
        h.register(
                SpringInfoPayload.class,
                (mc, p) -> {
                    if (mc.player != null)
                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "stellaeomphalos.spring.info", p.present(), p.fillBand()),
                                true);
                });
        h.register(StructureStatePayload.class, (mc, p) -> remember(STRUCTURES, p.origin(), p));
        h.register(RiteStatePayload.class, (mc, p) -> remember(RITES, p.origin(), p));
        h.register(
                RiteOutputPayload.class,
                (mc, p) -> {
                    if (mc.player != null)
                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "stellaeomphalos.rite.output",
                                        p.output().getHoverName(),
                                        p.output().getCount(),
                                        p.cycleIndex()),
                                true);
                });
        h.register(
                RetrogenStatusPayload.class,
                (mc, p) -> {
                    if (mc.player != null)
                        mc.player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        p.done()
                                                ? "stellaeomphalos.retrogen.done"
                                                : "stellaeomphalos.retrogen.status",
                                        p.processed(),
                                        p.queued(),
                                        p.skipped()),
                                !p.done());
                });
        h.register(
                StarfallNoticePayload.class,
                (mc, p) -> {
                    if (mc.player != null
                            && mc.level != null
                            && mc.level.dimension().location().equals(p.dimension()))
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
        ClientSessionCleaner.register("structure_previews", StructureMirror::clear);
        MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone event) -> clear());
        MinecraftForge.EVENT_BUS.addListener(StructureMirror::tick);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused())
            PREVIEWS.values().removeIf(s -> !s.tick());
    }
}
