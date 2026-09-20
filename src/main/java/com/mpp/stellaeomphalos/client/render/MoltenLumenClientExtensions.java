package com.mpp.stellaeomphalos.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Placeholder textures are expected to be missing until the resource request sheets land. */
public final class MoltenLumenClientExtensions implements IClientFluidTypeExtensions {
    public static final ResourceLocation STILL_TEXTURE = new ResourceLocation("stellaeomphalos", "block/molten_lumen_still");
    public static final ResourceLocation FLOW_TEXTURE = new ResourceLocation("stellaeomphalos", "block/molten_lumen_flow");
    public static final int TINT = 0xFFD9ECFF;

    @Override
    public ResourceLocation getStillTexture() { return STILL_TEXTURE; }

    @Override
    public ResourceLocation getFlowingTexture() { return FLOW_TEXTURE; }

    @Override
    public int getTintColor() { return TINT; }
}
