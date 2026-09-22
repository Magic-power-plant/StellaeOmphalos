package com.mpp.stellaeomphalos.client.render.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

/** Atlas-based fluid visuals use the actual fluid sprite and tint, with bounded fill fractions. */
public final class FluidRenderHelper {
    private FluidRenderHelper() {}

    public static void render(WorldDraw draw, FluidStack fluid, int capacity, boolean cube) {
        if (fluid.isEmpty() || capacity <= 0) return;
        var properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture = properties.getStillTexture(fluid);
        var sprite =
                Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(texture == null ? MissingTextureAtlasSprite.getLocation() : texture);
        int tint = properties.getTintColor(fluid),
                color = (Math.min(180, tint >>> 24) << 24) | (tint & 0xffffff);
        double fill = Math.min(1, (double) fluid.getAmount() / capacity);
        var vertices = draw.spriteBuffer(sprite);
        int light = draw.light;
        draw.light = LightTexture.FULL_BRIGHT;
        try {
            if (cube) draw.box(vertices, 0, 0, 0, .25, .25 * fill, .25, color);
            else draw.quad(vertices, 0, .32 + .6 * fill, 0, -.34, 0, 0, 0, 0, .34, color);
        } finally {
            draw.light = light;
        }
    }
}
