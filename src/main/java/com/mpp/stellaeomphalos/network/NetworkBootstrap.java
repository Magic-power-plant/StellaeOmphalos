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
        bus.addListener(NetworkBootstrap::setup);
    }
    private static void setup(FMLCommonSetupEvent event) { event.enqueueWork(OmphalosChannel::initialize); }
}
