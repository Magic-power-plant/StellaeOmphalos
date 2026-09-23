package com.mpp.stellaeomphalos.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mpp.stellaeomphalos.client.render.model.MachineMeshes;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.content.item.ClientRenderedItem;
import com.mpp.stellaeomphalos.content.item.CatalogItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Uses Forge's per-item hook; it neither swaps global renderers nor constructs fake block entities.
 */
public final class StackRenderBypass {
    private StackRenderBypass() {}

    public static void install() {
        var mc = Minecraft.getInstance();
        var renderer = new TelescopeItemRenderer(mc);
        ((ClientRenderedItem) CatalogItems.HAND_SPYGLASS.get())
                .installClientExtensions(
                        new IClientItemExtensions() {
                            @Override
                            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                                return renderer;
                            }
                        });
    }

    public static void bake(ModelEvent.ModifyBakingResult event) {
        var id = new ModelResourceLocation(CatalogItems.HAND_SPYGLASS.id(), "inventory");
        var original = event.getModels().get(id);
        if (original != null) event.getModels().put(id, new CodeModel(original));
    }

    private static final class CodeModel
            extends net.minecraftforge.client.model.BakedModelWrapper<BakedModel> {
        CodeModel(BakedModel model) {
            super(model);
        }

        @Override
        public boolean isCustomRenderer() {
            return true;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean left) {
            originalModel.applyTransform(context, pose, left);
            return this;
        }
    }

    private static final class TelescopeItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final WorldDraw draw = new WorldDraw();

        TelescopeItemRenderer(Minecraft mc) {
            super(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }

        @Override
        public void renderByItem(
                ItemStack stack,
                ItemDisplayContext context,
                PoseStack pose,
                MultiBufferSource buffers,
                int light,
                int overlay) {
            draw.pose = pose;
            draw.buffers = buffers;
            draw.light = light;
            draw.vertices = 0;
            pose.pushPose();
            try {
                pose.translate(.5, .5, .5);
                pose.mulPose(draw.rotate(.45F, .65F, 0));
                MachineMeshes.TELESCOPE.emit(draw, draw.solid(), 0xffc4b9a7);
            } finally {
                pose.popPose();
                draw.pose = null;
                draw.buffers = null;
            }
        }
    }
}
