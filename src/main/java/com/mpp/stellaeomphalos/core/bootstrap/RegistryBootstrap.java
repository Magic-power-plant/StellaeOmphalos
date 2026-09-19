package com.mpp.stellaeomphalos.core.bootstrap;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.core.registry.*;
import com.mpp.stellaeomphalos.core.platform.CompatRegistry;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import java.util.List;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

public final class RegistryBootstrap {
    private RegistryBootstrap() {}
    public static List<RegistryFamily<?>> families() {
        return List.of(ModBlocks.ENTRIES, ModItems.ENTRIES, ModBlockEntities.ENTRIES,
                ModEntities.ENTRIES, ModFluids.FLUIDS, ModFluids.TYPES, ModParticles.ENTRIES,
                ModEffects.ENTRIES, ModEnchantments.ENTRIES, ModSounds.ENTRIES, ModMenus.ENTRIES,
                ModRecipeTypes.TYPES, ModRecipeTypes.SERIALIZERS, ModStructures.TYPES,
                ModStructures.PIECES, ModStructures.PROCESSORS, ModCreativeTabs.ENTRIES,
                ModAttributes.ENTRIES, ModDataSerializers.ENTRIES, ModFeatures.ENTRIES,
                ModPlacementModifiers.ENTRIES, ModTrunkPlacerTypes.ENTRIES, ModDecorators.ENTRIES);
    }
    public static void attach(IEventBus bus) {
        families().forEach(family -> family.attach(bus));
        bus.addListener(RegistryBootstrap::setup);
        bus.addListener(RegistryBootstrap::complete);
        DataBootstrap.attach(bus);
        RuntimeServices.attach();
    }
    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LifecycleOrchestrator.registriesReady();
            ModAdvancementTriggers.register();
            CompatRegistry.discover();
        });
    }
    private static void complete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            LifecycleOrchestrator.loadComplete();
            families().forEach(family -> LogUtils.getLogger().info("Registry {}: {} entries", family.key(), family.ids().size()));
        });
    }
}
