package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import com.mpp.stellaeomphalos.data.loader.DataTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Assembly point of the domain module. Declares the {@code domain_traits} data table, initializes
 * the effect registry, binds effects onto sign instances (again after every sign-table reload), and
 * wires the spawn-denial listener onto the Forge bus. The integrator calls
 * {@code DomainBootstrap.attach(modBus)} from the mod constructor; there are no C2S handlers.
 */
public final class DomainBootstrap {
    public static final DataTable<DomainTraits.TraitTable> DOMAIN_TRAITS =
            DataBootstrap.TABLES.declare(new DataTable<>(new ResourceLocation(Omphalos.MODID, "domain_traits"),
                    DomainTraits.TraitTable.CODEC, DomainBootstrap::validate));

    private DomainBootstrap() {}

    private static void validate(java.util.Map<ResourceLocation, DomainTraits.TraitTable> entries,
                                 com.mpp.stellaeomphalos.data.loader.DataLoadReport report) {
        entries.forEach((file, table) -> table.traits().forEach((name, modifier) -> {
            if (modifier.potencyScale() <= 0 || modifier.sizeScale() <= 0 || modifier.amplifierScale() <= 0)
                report.error(file.toString(), "$.traits." + name, "Scales must be positive");
            if (modifier.fractureLowerScale() < 0 || modifier.fractureRateScale() < 0)
                report.error(file.toString(), "$.traits." + name, "Fracture scales must be nonnegative");
        }));
    }

    public static void attach(IEventBus modBus) {
        var forge = MinecraftForge.EVENT_BUS;
        forge.addListener(DomainBootstrap::finalizeSpawn);
        forge.addListener(DomainBootstrap::serverStop);
        forge.addListener(DomainBootstrap::dataReloaded);
        // modBus is null in GameTest setups that only want the Forge-side hooks.
        if (modBus != null) modBus.addListener(DomainBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            DomainEffectRegistry.initialize();
            DomainEffectRegistry.bindToSigns();
        });
    }

    /**
     * Sign instances are replaced wholesale on reload (SignBootstrap rebuilds the registry first —
     * listener registration order), so rebind effects onto the fresh instances afterwards.
     */
    private static void dataReloaded(AddReloadListenerEvent event) {
        event.addListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager ->
                DomainEffectRegistry.bindToSigns());
    }

    private static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        DomainSpawnDeny.onFinalizeSpawn(event);
    }

    private static void serverStop(ServerStoppingEvent event) {
        DomainSpawnDeny.clear();
        DomainParticles.resetGates();
    }
}
