package com.mpp.stellaeomphalos.lumen.transport;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.network.PayloadHandlers;
import com.mpp.stellaeomphalos.network.toServer.PktLumenSinkQuery;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Lumen module assembly. Forge-bus subscriptions (scheduler, topology, session) self-attach via
 * annotations; capability registration lives in {@code LumenCaps}. Integrator hook:
 * `LumenBootstrap.attach(modBus)` in the Omphalos constructor (freezes the sink registry).
 */
@Mod.EventBusSubscriber(modid = Omphalos.MODID)
public final class LumenBootstrap {
    private static PayloadHandlers<ServerPlayer> attachedHandlers;
    private LumenBootstrap() {}

    /** Integrator hook: append in the Omphalos constructor after RegistryBootstrap. */
    public static void attach(IEventBus modBus) {
        modBus.addListener(LumenBootstrap::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(LumenSinkRegistry::freeze);
    }

    /** LOW so RuntimeServices' own ServerAboutToStart listener (NORMAL) has already run. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void serverAboutToStart(ServerAboutToStartEvent event) {
        LumenSession.advance();
        attachServer();
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent event) {
        LumenServerHandlers.clear();
    }

    /** Registers the C2S lumen handler set into the current server session; idempotent per session. */
    public static synchronized void attachServer() {
        var handlers = RuntimeServices.current().handlers();
        if (handlers == attachedHandlers) return;
        handlers.register(PktLumenSinkQuery.class, LumenServerHandlers::sinkQuery);
        attachedHandlers = handlers;
    }
}
