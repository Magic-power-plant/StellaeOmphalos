package com.mpp.stellaeomphalos.lumen.transport.stasis;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Assembly hooks for stasis. Entity freezing uses LivingEvent.LivingTickEvent (cancelable) for
 * living entities; Forge 47.4.23 has no EntityTickEvent, so non-living entities are pinned by
 * position/motion compensation inside StasisZone (see its javadoc; [待验证] for equivalence).
 *
 * Potion timers and hurt cooldowns are deliberately NOT frozen: they are advanced manually
 * before the tick is canceled. An ender dragon in the DYING phase is forced to HOLDING_PATTERN.
 */
public final class StasisBootstrap {
    private static final AtomicBoolean ATTACHED = new AtomicBoolean();
    private StasisBootstrap() {}

    /** modBus is unused (no mod-bus registrations); listeners live on the Forge event bus. Idempotent. */
    public static void attach(IEventBus modBus) {
        if (!ATTACHED.compareAndSet(false, true)) return;
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(StasisBootstrap::onLivingTick);
        bus.addListener(StasisBootstrap::onLevelTick);
        bus.addListener(StasisBootstrap::onStopping);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        var service = StasisService.get(level.getServer());
        if (!service.isFrozen(entity)) return;
        if (entity.hurtTime > 0) entity.hurtTime--;
        if (entity.invulnerableTime > 0) entity.invulnerableTime--;
        for (var instance : new ArrayList<>(entity.getActiveEffects()))
            if (!instance.tick(entity, () -> {})) entity.removeEffect(instance.getEffect());
        if (entity instanceof EnderDragon dragon
                && dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING)
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        event.setCanceled(true);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (!com.mpp.stellaeomphalos.OmphalosConfig.SERVER.flag("gameplay.stasisEnabled")) return;
        StasisService.get(level.getServer()).tick(level);
    }

    private static void onStopping(ServerStoppingEvent event) { StasisService.shutdown(); }
}
