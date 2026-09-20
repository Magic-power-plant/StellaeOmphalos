package com.mpp.stellaeomphalos.player.charge;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Assembly hooks for the charge module. The integrator calls {@link #attach} from the Omphalos
 * constructor; the client hook is com.mpp.stellaeomphalos.client.charge.ChargeMirror.attach()
 * (called from OmphalosClient.setup). Attach is idempotent so GameTests may call it directly
 * when the integrator has not wired it yet.
 */
public final class ChargeBootstrap {
    private static final AtomicBoolean ATTACHED = new AtomicBoolean();
    private ChargeBootstrap() {}

    /** modBus is unused (no mod-bus registrations); listeners live on the Forge event bus. */
    public static void attach(IEventBus modBus) {
        if (!ATTACHED.compareAndSet(false, true)) return;
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(ChargeBootstrap::onPlayerTick);
        bus.addListener(ChargeBootstrap::onLogin);
        bus.addListener(ChargeBootstrap::onLogout);
        bus.addListener(ChargeBootstrap::onStopping);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer player)
            StarlightChargeService.get(player.server).tick(player);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            StarlightChargeService.get(player.server).bumpSession(player.getUUID());
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            StarlightChargeService.get(player.server).onDisconnect(player.getUUID());
    }

    private static void onStopping(ServerStoppingEvent event) { StarlightChargeService.shutdown(); }
}
