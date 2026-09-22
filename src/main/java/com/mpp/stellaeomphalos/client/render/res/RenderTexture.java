package com.mpp.stellaeomphalos.client.render.res;

import net.minecraft.resources.ResourceLocation;

public interface RenderTexture {
    ResourceLocation location();

    float u0();

    float v0();

    float u1();

    float v1();

    boolean isReady();
}
