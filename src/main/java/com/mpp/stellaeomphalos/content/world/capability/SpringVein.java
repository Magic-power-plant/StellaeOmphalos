package com.mpp.stellaeomphalos.content.world.capability;

import net.minecraft.resources.ResourceLocation;

public interface SpringVein {
    boolean present();

    ResourceLocation fluidId();

    int capacityMb();

    int remainingMb();

    int drain(int mb, boolean consume);

    void invalidate();
}
