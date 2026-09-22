package com.mpp.stellaeomphalos.client.render.ber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.client.render.DeferredEffectQueue;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Lifecycle and culling are final; specialized renderers only describe geometry. */
public abstract class OmphalosBlockEntityRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T> {
    private final int baseDistance;

    protected OmphalosBlockEntityRenderer(int distance) {
        baseDistance = distance;
    }

    @Override
    public final int getViewDistance() {
        return Math.max(
                16, baseDistance * OmphalosConfig.CLIENT.integer("render.beRendererDistance") / 64);
    }

    @Override
    public boolean shouldRender(T be, Vec3 camera) {
        if (be.isRemoved() || be.getLevel() == null) return false;
        var p = be.getBlockPos();
        double x = p.getX() + .5 - camera.x,
                y = p.getY() + .5 - camera.y,
                z = p.getZ() + .5 - camera.z;
        return x * x + y * y + z * z <= (double) getViewDistance() * getViewDistance()
                && DeferredEffectQueue.visible(be.getRenderBoundingBox());
    }

    @Override
    public final void render(
            T be,
            float partial,
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            int overlay) {
        if (shouldRender(be, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()))
            DeferredEffectQueue.enqueue(this, be, partial, light, overlay);
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return false;
    }

    public final void draw(T be, WorldDraw draw, int light, int overlay) {
        draw.pose.pushPose();
        try {
            var p = be.getBlockPos();
            draw.pose.translate(p.getX() + .5, p.getY(), p.getZ() + .5);
            draw.light = light;
            renderModel(be, draw, light, overlay);
        } finally {
            draw.pose.popPose();
        }
    }

    protected abstract void renderModel(T be, WorldDraw draw, int light, int overlay);
}
