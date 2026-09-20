package com.mpp.stellaeomphalos.lumen.transport;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.OmphalosConfig;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Per-tick lumen budget driver (ServerTickEvent END): topology ops, BFS edge steps and a hard
 * wall-clock cap via nanoTime. With `gameplay.lumenEnabled = false` the whole system idles (AC-2.23).
 */
@Mod.EventBusSubscriber(modid = Omphalos.MODID)
public final class LumenScheduler {
    private LumenScheduler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!OmphalosConfig.SERVER.flag("gameplay.lumenEnabled")) return;
        var server = event.getServer();
        if (server == null) return;
        long deadline = System.nanoTime() + OmphalosConfig.COMMON.integer("performance.lumenTickBudgetMicros") * 1000L;
        LumenTopology.tick(server,
                OmphalosConfig.COMMON.integer("performance.lumenTopologyOpsPerTick"),
                OmphalosConfig.COMMON.integer("performance.lumenProximityOpsPerTick"),
                OmphalosConfig.COMMON.integer("performance.lumenRoutingStepsPerTick"),
                deadline);
    }
}
