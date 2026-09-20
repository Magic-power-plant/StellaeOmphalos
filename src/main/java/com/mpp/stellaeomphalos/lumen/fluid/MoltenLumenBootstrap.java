package com.mpp.stellaeomphalos.lumen.fluid;

import com.mpp.stellaeomphalos.Omphalos;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidInteractionRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Assembly point for molten lumen. The annotation self-loads the module until the integrator wires
 * attach(IEventBus) into the Omphalos constructor; attach is idempotent with the annotation.
 */
@Mod.EventBusSubscriber(modid = Omphalos.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MoltenLumenBootstrap {
    static {
        MoltenLumenContent.init();
    }

    private MoltenLumenBootstrap() {}

    public static void attach(IEventBus modBus) {
        MoltenLumenContent.init();
    }

    @SubscribeEvent
    static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> FluidInteractionRegistry.addInteraction(
                MoltenLumenContent.TYPE.get(), LumenFluidInteractions.interactionInfo()));
    }
}
