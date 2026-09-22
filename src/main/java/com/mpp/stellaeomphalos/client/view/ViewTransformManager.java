package com.mpp.stellaeomphalos.client.view;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.OmphalosConfig;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A client-only vanilla probe never joins the world. Player position and orientation are untouched.
 */
@Mod.EventBusSubscriber(modid = Omphalos.MODID, value = Dist.CLIENT)
public final class ViewTransformManager {
    private record Settings(Entity camera, boolean hideGui, boolean bob, CameraType cameraType) {}

    private static CameraController active;
    private static Settings settings;
    private static ArmorStand probe;
    private static final org.joml.Vector3d POSITION = new org.joml.Vector3d();

    private ViewTransformManager() {}

    public static boolean start(CameraController sequence) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !OmphalosConfig.CLIENT.flag("view.sequences"))
            return false;
        if (active != null && active.priority() > sequence.priority()) return false;
        stopAll();
        settings =
                new Settings(
                        mc.getCameraEntity(),
                        mc.options.hideGui,
                        mc.options.bobView().get(),
                        mc.options.getCameraType());
        probe = new ArmorStand(mc.level, mc.player.getX(), mc.player.getY(), mc.player.getZ());
        probe.setInvisible(true);
        probe.setNoGravity(true);
        active = sequence;
        active.sample(0, POSITION);
        applyPosition();
        probe.xo = probe.getX();
        probe.yo = probe.getY();
        probe.zo = probe.getZ();
        mc.options.hideGui = true;
        mc.options.bobView().set(false);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(probe);
        return true;
    }

    private static void applyPosition() {
        probe.setPos(POSITION.x, POSITION.y - probe.getEyeHeight(), POSITION.z);
        probe.setYRot(active.yaw());
        probe.setXRot(active.pitch());
    }

    public static void tick() {
        if (active == null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null
                || !mc.player.isAlive()
                || mc.level != probe.level()
                || !OmphalosConfig.CLIENT.flag("view.sequences")) {
            stopAll();
            return;
        }
        probe.xo = probe.getX();
        probe.yo = probe.getY();
        probe.zo = probe.getZ();
        active.tick();
        active.sample(0, POSITION);
        applyPosition();
        if (active.finished()) stopAll();
    }

    public static void stopAll() {
        if (active == null) return;
        active.stop();
        var mc = Minecraft.getInstance();
        mc.options.hideGui = settings.hideGui();
        mc.options.bobView().set(settings.bob());
        mc.options.setCameraType(settings.cameraType());
        mc.setCameraEntity(
                settings.camera() != null && settings.camera().level() == mc.level
                        ? settings.camera()
                        : mc.player);
        active = null;
        settings = null;
        probe = null;
    }

    public static boolean isActive() {
        return active != null;
    }

    @SubscribeEvent
    public static void angles(ViewportEvent.ComputeCameraAngles event) {
        if (active != null) {
            active.sample((float) event.getPartialTick(), POSITION);
            event.setYaw(active.yaw());
            event.setPitch(active.pitch());
            event.setRoll(0);
        }
    }

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov event) {
        if (active != null) event.setFOV(active.fov());
    }

    @SubscribeEvent
    public static void hand(net.minecraftforge.client.event.RenderHandEvent event) {
        if (active != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void interact(
            net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        if (active != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void input(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        if (active != null) {
            var input = event.getInput();
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }
}
