package com.mpp.stellaeomphalos.lumen.capability;

import com.mpp.stellaeomphalos.Omphalos;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge 47 idiom: RegisterCapabilitiesEvent only registers capability types. */
@Mod.EventBusSubscriber(modid = Omphalos.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LumenCaps {
    private LumenCaps() {}

    @SubscribeEvent
    public static void onRegisterCaps(RegisterCapabilitiesEvent event) {
        event.register(LumenHandler.class);
    }
}
