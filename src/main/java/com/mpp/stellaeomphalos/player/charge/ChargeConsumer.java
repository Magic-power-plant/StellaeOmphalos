package com.mpp.stellaeomphalos.player.charge;

import net.minecraft.server.level.ServerPlayer;

/**
 * A consumer of starlight charge (runed tools, later parts).
 *
 * Two-phase semantics: phase one probes with {@code simulate=true}; only when the probe
 * succeeds may phase two commit with {@code simulate=false}. Consumers must never commit
 * without a successful probe, and must not mutate their own state between the two phases
 * in a way that would invalidate the probed amount.
 */
public interface ChargeConsumer {
    default boolean consume(ServerPlayer player, float amount) {
        var service = StarlightChargeService.get(player.server);
        if (!service.drain(player, amount, true)) return false;
        return service.drain(player, amount, false);
    }
}
