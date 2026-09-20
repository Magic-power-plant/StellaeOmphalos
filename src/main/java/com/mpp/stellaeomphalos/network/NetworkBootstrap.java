package com.mpp.stellaeomphalos.network;

import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.network.toServer.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public final class NetworkBootstrap {
    private NetworkBootstrap() {}
    public static void attach(IEventBus bus) {
        var registry = OmphalosChannel.PAYLOADS;
        registry.register(0, PayloadRegistry.Direction.TO_CLIENT, PktSyncGateOpen.class, PktSyncGateOpen.CODEC, 0);
        registry.register(1, PayloadRegistry.Direction.TO_CLIENT, PktSyncGateClose.class, PktSyncGateClose.CODEC, 0);
        registry.register(2, PayloadRegistry.Direction.TO_SERVER, PktClientSyncReady.class, PktClientSyncReady.CODEC, 0);
        registry.register(3, PayloadRegistry.Direction.TO_CLIENT, PktSyncDataset.class, PktSyncDataset.CODEC, 0);
        registry.register(4, PayloadRegistry.Direction.TO_SERVER, PktDatasetRequest.class, PktDatasetRequest.CODEC, 0);
        registry.register(5, PayloadRegistry.Direction.TO_CLIENT, PktConfigVersion.class, PktConfigVersion.CODEC, 0);
        registry.register(6, PayloadRegistry.Direction.TO_CLIENT, PktThrottleNotice.class, PktThrottleNotice.CODEC, 0);
        registry.register(7, PayloadRegistry.Direction.TO_SERVER, PktNetworkDebugDumpRequest.class, PktNetworkDebugDumpRequest.CODEC, 0);
        registry.register(8, PayloadRegistry.Direction.TO_CLIENT, PktNetworkDebugDump.class, PktNetworkDebugDump.CODEC, 0);
        registry.register(9, PayloadRegistry.Direction.TO_CLIENT, PktMigrationReport.class, PktMigrationReport.CODEC, 0);
        registry.register(10, PayloadRegistry.Direction.TO_CLIENT, PktChargeSync.class, PktChargeSync.CODEC, 0);
        registry.register(11, PayloadRegistry.Direction.TO_SERVER, PktSkySeedRequest.class, PktSkySeedRequest.CODEC, 0);
        registry.register(12, PayloadRegistry.Direction.TO_CLIENT, PktSkySeed.class, PktSkySeed.CODEC, 0);
        registry.register(13, PayloadRegistry.Direction.TO_CLIENT, PktActiveSigns.class, PktActiveSigns.CODEC, 0);
        registry.register(14, PayloadRegistry.Direction.TO_CLIENT, PktSignSkyLayout.class, PktSignSkyLayout.CODEC, 0);
        registry.register(15, PayloadRegistry.Direction.TO_CLIENT, PktSignRename.class, PktSignRename.CODEC, 0);
        registry.register(16, PayloadRegistry.Direction.TO_CLIENT, PktBoonTreeSync.class, PktBoonTreeSync.CODEC, 0);
        registry.register(17, PayloadRegistry.Direction.TO_CLIENT, PktBoonDelta.class, PktBoonDelta.CODEC, 0);
        registry.register(18, PayloadRegistry.Direction.TO_CLIENT, PktBoonExp.class, PktBoonExp.CODEC, 0);
        registry.register(19, PayloadRegistry.Direction.TO_CLIENT, PktStasisZone.class, PktStasisZone.CODEC, 0);
        registry.register(20, PayloadRegistry.Direction.TO_CLIENT, PktLumenNode.class, PktLumenNode.CODEC, 0);
        registry.register(21, PayloadRegistry.Direction.TO_CLIENT, PktLumenDelta.class, PktLumenDelta.CODEC, 0);
        registry.register(22, PayloadRegistry.Direction.TO_SERVER, PktLumenSinkQuery.class, PktLumenSinkQuery.CODEC, 0);
        registry.register(23, PayloadRegistry.Direction.TO_SERVER, PktImprintEngrave.class, PktImprintEngrave.CODEC, 0);
        registry.register(24, PayloadRegistry.Direction.TO_CLIENT, PktDomainParticle.class, PktDomainParticle.CODEC, 0);
        bus.addListener(NetworkBootstrap::setup);
    }
    private static void setup(FMLCommonSetupEvent event) { event.enqueueWork(OmphalosChannel::initialize); }
}
