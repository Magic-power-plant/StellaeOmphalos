package com.mpp.stellaeomphalos.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.content.entity.catalog.*;
import com.mpp.stellaeomphalos.content.world.WorldContent;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** All fifteen entity types have an explicit renderer; data-only seats remain invisible. */
public final class VisualEntityRenderers {
    private VisualEntityRenderers() {}

    private static <T extends Entity> net.minecraft.world.entity.EntityType<T> entity(
            RegistrationGuard<net.minecraft.world.entity.EntityType<T>> guard) {
        var type = ForgeRegistries.ENTITY_TYPES.getValue(guard.id());
        if (type == null)
            throw new IllegalStateException("Missing entity renderer target " + guard.id());
        @SuppressWarnings("unchecked")
        var cast = (net.minecraft.world.entity.EntityType<T>) type;
        return cast;
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(entity(CatalogEntities.HIGHLIGHTED_ITEM), Highlight::new);
        e.registerEntityRenderer(
                entity(CatalogEntities.EXPLOSION_PROOF_ITEM), ItemEntityRenderer::new);
        e.registerEntityRenderer(entity(CatalogEntities.STAR_DUST), ItemEntityRenderer::new);
        e.registerEntityRenderer(entity(CatalogEntities.GEODE_ENTITY), ItemEntityRenderer::new);
        e.registerEntityRenderer(
                entity(CatalogEntities.GEODE_TOOL_ENTITY), ItemEntityRenderer::new);
        e.registerEntityRenderer(entity(CatalogEntities.GRAPNEL), Grapple::new);
        e.registerEntityRenderer(entity(CatalogEntities.LUCENT_SPARK), Glow::new);
        e.registerEntityRenderer(entity(CatalogEntities.UMBRAL_SPARK), Glow::new);
        e.registerEntityRenderer(entity(CatalogEntities.STAR_BOLT), Glow::new);
        e.registerEntityRenderer(entity(CatalogEntities.FALLING_STAR), Glow::new);
        e.registerEntityRenderer(entity(CatalogEntities.OBSERVATORY_SEAT), NoopRenderer::new);
        e.registerEntityRenderer(entity(CatalogEntities.WISP), Glow::new);
        e.registerEntityRenderer(entity(CatalogEntities.LUMEN_DROPLET), Glow::new);
        e.registerEntityRenderer(entity(CatalogEntities.PHANTOM_TOOL), Phantom::new);
        e.registerEntityRenderer(entity(WorldContent.STARFALL), Glow::new);
    }

    private static class Glow<T extends Entity> extends EntityRenderer<T> {
        protected final WorldDraw draw = new WorldDraw();

        Glow(EntityRendererProvider.Context c) {
            super(c);
            shadowRadius = 0;
        }

        public ResourceLocation getTextureLocation(T e) {
            return OmphalosRenderTypes.TEXTURE;
        }

        protected void begin(PoseStack pose, MultiBufferSource buffers, float partial) {
            draw.pose = pose;
            draw.buffers = buffers;
            draw.partial = partial;
            draw.vertices = 0;
        }

        protected void end() {
            draw.pose = null;
            draw.buffers = null;
        }

        @Override
        public void render(
                T e,
                float yaw,
                float partial,
                PoseStack pose,
                MultiBufferSource buffers,
                int light) {
            begin(pose, buffers, partial);
            try {
                var v = draw.buffer(OmphalosRenderTypes.Kind.GLOW_LAYER);
                draw.box(v, 0, .1, 0, .08, .08, .08, 0xffbcdfff);
                var speed = e.getDeltaMovement();
                if (speed.lengthSqr() > .01
                        && com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.flag("sky.meteorTrails"))
                    draw.beam(
                            v,
                            0,
                            .1,
                            0,
                            -speed.x * 5,
                            .1 - speed.y * 5,
                            -speed.z * 5,
                            .04,
                            0x66aabfff);
            } finally {
                end();
            }
        }
    }

    private static final class Grapple extends Glow<GrapnelEntity> {
        Grapple(EntityRendererProvider.Context c) {
            super(c);
        }

        @Override
        public void render(
                GrapnelEntity e,
                float yaw,
                float partial,
                PoseStack pose,
                MultiBufferSource buffers,
                int light) {
            super.render(e, yaw, partial, pose, buffers, light);
            var owner = e.getOwner();
            if (owner == null) return;
            begin(pose, buffers, partial);
            try {
                draw.beam(
                        draw.buffer(OmphalosRenderTypes.Kind.BEAM_ADDITIVE),
                        0,
                        0,
                        0,
                        owner.getX() - e.getX(),
                        owner.getEyeY() - .4 - e.getY(),
                        owner.getZ() - e.getZ(),
                        .015,
                        0x99ccdcff);
            } finally {
                end();
            }
        }
    }

    private static final class Highlight extends ItemEntityRenderer {
        private final WorldDraw draw = new WorldDraw();

        Highlight(EntityRendererProvider.Context c) {
            super(c);
        }

        @Override
        public void render(
                ItemEntity e,
                float yaw,
                float partial,
                PoseStack pose,
                MultiBufferSource buffers,
                int light) {
            super.render(e, yaw, partial, pose, buffers, light);
            draw.pose = pose;
            draw.buffers = buffers;
            draw.vertices = 0;
            try {
                int color =
                        e instanceof HighlightedItemEntity h && h.hasColor()
                                ? h.getColor()
                                : com.mpp.stellaeomphalos.client.image.PaletteTable.colorOf(
                                        e.getItem());
                draw.beam(
                        draw.buffer(OmphalosRenderTypes.Kind.BEAM_ADDITIVE),
                        0,
                        .1,
                        0,
                        0,
                        2,
                        0,
                        .025,
                        0x44000000 | (color & 0xffffff));
            } finally {
                draw.pose = null;
                draw.buffers = null;
            }
        }
    }

    private static final class Phantom extends Glow<PhantomToolEntity> {
        private final net.minecraft.client.renderer.entity.ItemRenderer items;

        Phantom(EntityRendererProvider.Context c) {
            super(c);
            items = c.getItemRenderer();
        }

        @Override
        public void render(
                PhantomToolEntity e,
                float yaw,
                float partial,
                PoseStack pose,
                MultiBufferSource buffers,
                int light) {
            pose.pushPose();
            try {
                pose.translate(0, .35, 0);
                items.renderStatic(
                        e.getTool(),
                        net.minecraft.world.item.ItemDisplayContext.GROUND,
                        0xf000f0,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        pose,
                        buffers,
                        e.level(),
                        e.getId());
            } finally {
                pose.popPose();
            }
            super.render(e, yaw, partial, pose, buffers, light);
        }
    }
}
