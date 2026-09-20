package com.mpp.stellaeomphalos.lumen.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/** Holder for the lumen capability instance; valid after LumenCaps registration ran. */
public final class LumenCapability {
    public static final Capability<LumenHandler> LUMEN = CapabilityManager.get(new CapabilityToken<>() {});
    private LumenCapability() {}
}
