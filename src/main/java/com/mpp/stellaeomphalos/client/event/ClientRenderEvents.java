package com.mpp.stellaeomphalos.client.event;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.client.effect.*;
import com.mpp.stellaeomphalos.client.render.*;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.client.sky.StarfieldRenderer;
import com.mpp.stellaeomphalos.client.sound.MachineSoundHost;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One world dispatcher, four geometry anchors, one session ownership boundary. */
@Mod.EventBusSubscriber(modid = Omphalos.MODID, value = Dist.CLIENT)
public final class ClientRenderEvents {
    public static final EffectDirector EFFECTS = new EffectDirector();
    private static final WorldDraw DRAW = new WorldDraw();
    private static final net.minecraft.client.renderer.MultiBufferSource.BufferSource BUFFERS =
            net.minecraft.client.renderer.MultiBufferSource.immediate(
                    new com.mojang.blaze3d.vertex.BufferBuilder(262144));
    private static int worldFrames, worldVertices,ghostVertices;
    public static int ghostVertices(){return ghostVertices;}

    public static int worldFrames() {
        return worldFrames;
    }

    public static int worldVertices() {
        return worldVertices;
    }

    private static net.minecraft.client.multiplayer.ClientLevel level;
    private static int ticks;

    private ClientRenderEvents() {}

    public static void clear() {
        com.mpp.stellaeomphalos.client.sound.EntitySoundHost.clear();
        com.mpp.stellaeomphalos.client.render.obj.ObjMeshLibrary.closeAll();
        com.mpp.stellaeomphalos.client.view.ViewCaptureCache.clear();
        com.mpp.stellaeomphalos.client.screen.AstrolabeTargetCache.clear();
        com.mpp.stellaeomphalos.client.hud.LumenBarHud.clear();
        com.mpp.stellaeomphalos.client.image.PaletteTable.clear();
        com.mpp.stellaeomphalos.client.view.ViewTransformManager.stopAll();
        EFFECTS.clearAll();
        DeferredEffectQueue.clear();
        PhantomBatch.clear();
        StarfieldRenderer.clear();
        MachineSoundHost.clear();
        com.mpp.stellaeomphalos.client.particle.ParticleSpawner.clear();
        com.mpp.stellaeomphalos.client.sound.UiSounds.clear();
        com.mpp.stellaeomphalos.client.render.res.TextureStore.clear();
        level = null;
        ticks = 0;
    }

    @SubscribeEvent
    public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clear();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level != level) {
            clear();
            level = mc.level;
        }
        if (level == null || mc.isPaused()) return;
        ticks++;
        com.mpp.stellaeomphalos.client.hud.LumenBarHud.tick();
        com.mpp.stellaeomphalos.client.view.ViewTransformManager.tick();
        ShaderCompat.refresh();
        MachineSoundHost.tick();
        com.mpp.stellaeomphalos.client.sound.EntitySoundHost.tick();
        EFFECTS.budget(OmphalosConfig.CLIENT.integer("effects.budget"));
        EFFECTS.tickAll();
        if (ticks % 20 == 1) {
            StarfieldRenderer.tick();
            com.mpp.stellaeomphalos.client.image.PaletteTable.warm(mc.player.getMainHandItem());
        }
        if (OmphalosConfig.CLIENT.flag("effects.enabled")) {
            if (ticks % 20 == 0 && OmphalosConfig.CLIENT.flag("effects.orbits")) {
                for (var zone : com.mpp.stellaeomphalos.client.stasis.StasisMirror.zones()) {
                    var p = zone.center();
                    EFFECTS.spawn(
                            new QuadTrack(
                                    EffectLane.WORLD,
                                    p.getX() + .5,
                                    p.getY() + .1,
                                    p.getZ() + .5,
                                    zone.radius(),
                                    0x448fbfff,
                                    21,
                                    false));
                }
            }
            if (ticks % 40 == 1 && OmphalosConfig.CLIENT.flag("effects.beams"))
                for (var node : com.mpp.stellaeomphalos.client.lumen.LumenLinkMirror.links())
                    for (var target : node.connections()) {
                        var p = node.pos();
                        EFFECTS.spawn(
                                new BeamTrack(
                                        p.getX() + .5,
                                        p.getY() + .7,
                                        p.getZ() + .5,
                                        target.getX() + .5,
                                        target.getY() + .7,
                                        target.getZ() + .5,
                                        .02,
                                        0x669bceff,
                                        41));
                    }
            for (var request : DomainParticleMirror.drain()) {
                var p = request.pos();
                var target = request.target();
                if (target != null && OmphalosConfig.CLIENT.flag("effects.arcs"))
                    EFFECTS.spawn(
                            new ArcTrack(
                                    request.seed(),
                                    p.getX() + .5,
                                    p.getY() + .5,
                                    p.getZ() + .5,
                                    target.getX() + .5,
                                    target.getY() + .5,
                                    target.getZ() + .5,
                                    0xffc9dfff));
                else
                    EFFECTS.spawn(
                            new QuadTrack(
                                    EffectLane.WORLD,
                                    p.getX() + .5,
                                    p.getY() + .05,
                                    p.getZ() + .5,
                                    .5,
                                    0x889bcfff,
                                    25,
                                    false));
            }
        } else DomainParticleMirror.drain();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent e) {
        var stage = e.getStage();
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        EffectLane lane;
        if (stage == RenderLevelStageEvent.Stage.AFTER_SKY) {
            lane = EffectLane.SKY;
            DeferredEffectQueue.begin(e.getFrustum());
        } else if (stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) lane = EffectLane.GHOST;
        else if (stage == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            lane = EffectLane.WORLD;
        else if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES) lane = EffectLane.THROUGH;
        else return;
        var camera = e.getCamera().getPosition();
        var pose = e.getPoseStack();
        var buffers = BUFFERS;
        DRAW.pose = pose;
        DRAW.buffers = buffers;
        DRAW.cameraX = camera.x;
        DRAW.cameraY = camera.y;
        DRAW.cameraZ = camera.z;
        DRAW.partial = e.getPartialTick();
        DRAW.millis = net.minecraft.Util.getMillis();
        DRAW.vertices = 0;
        DRAW.light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        pose.pushPose();
        try {
            if (lane == EffectLane.SKY) StarfieldRenderer.render(DRAW);
            else pose.translate(-camera.x, -camera.y, -camera.z);
            if (lane == EffectLane.WORLD) {
                DeferredEffectQueue.flush(DRAW);
                worldFrames++;
            }
            DRAW.light = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
            double distance = OmphalosConfig.CLIENT.integer("effects.renderDistance");
            EFFECTS.flush(
                    lane, DRAW, distance * distance, OmphalosConfig.CLIENT.flag("effects.enabled"));
        } finally {
            pose.popPose();
            buffers.endBatch();
            if (lane == EffectLane.WORLD) worldVertices = DRAW.vertices;
            if(lane==EffectLane.GHOST)ghostVertices=DRAW.vertices;
            DRAW.pose = null;
            DRAW.buffers = null;
        }
    }

    @SubscribeEvent
    public static void gatewayFov(net.minecraftforge.client.event.ViewportEvent.ComputeFov event) {
        if (Minecraft.getInstance().screen
                instanceof com.mpp.stellaeomphalos.client.screen.GatewayScreen screen)
            event.setFOV(event.getFOV() * (1 - .3 * screen.chargeFraction()));
    }

    @SubscribeEvent
    public static void debug(net.minecraftforge.client.event.CustomizeGuiOverlayEvent.DebugText e) {
        if (OmphalosConfig.CLIENT.flag("debug.showEffectStats"))
            e.getLeft()
                    .add(
                            "Stellae: effects="
                                    + EFFECTS.trackCount()
                                    + " dropped="
                                    + EFFECTS.dropped()
                                    + " vertices="
                                    + DRAW.vertices
                                    + " particles="
                                    + com.mpp.stellaeomphalos.client.particle.ParticleSpawner
                                            .alive());
    }
}
