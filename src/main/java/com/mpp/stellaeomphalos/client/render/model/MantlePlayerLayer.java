package com.mpp.stellaeomphalos.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.content.item.CatalogItems;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;

/** Reuses vanilla player skeleton; mantle geometry follows the torso and crouch pose. */
public final class MantlePlayerLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final WorldDraw draw = new WorldDraw();

    public MantlePlayerLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            AbstractClientPlayer player,
            float limbSwing,
            float limbAmount,
            float partial,
            float age,
            float yaw,
            float pitch) {
        if (player.isInvisible()
                || !player.getItemBySlot(EquipmentSlot.CHEST).is(CatalogItems.MANTLE.get())) return;
        draw.pose = pose;
        draw.buffers = buffers;
        draw.light = light;
        draw.vertices = 0;
        pose.pushPose();
        try {
            getParentModel().body.translateAndRotate(pose);
            pose.translate(0, 0, .1);
            MachineMeshes.MANTLE_ARMOR.emit(draw, draw.solid(), 0xff666baf);
        } finally {
            pose.popPose();
            draw.pose = null;
            draw.buffers = null;
        }
    }
}
