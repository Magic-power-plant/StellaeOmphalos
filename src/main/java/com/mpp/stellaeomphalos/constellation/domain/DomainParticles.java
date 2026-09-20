package com.mpp.stellaeomphalos.constellation.domain;

import com.mpp.stellaeomphalos.constellation.sign.SignSkyService;
import com.mpp.stellaeomphalos.network.SafeDispatch;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Server-side dispatch of {@link PktDomainParticle} with the mandated high-frequency throttles
 * (plan 2.5):
 * <ul>
 *   <li>smelting-style events: same tick + same type + same position merge into at most 1 packet/tick
 *       ({@link #mergedThisTick});</li>
 *   <li>acceleration-style events: fixed interval limiter, 10 ticks by default ({@link #intervalPassed});</li>
 *   <li>spawn warmup: interval of at least 5 ticks (caller passes {@code interval >= 5}).</li>
 * </ul>
 */
public final class DomainParticles {
    private record GateKey(ResourceKey<Level> dimension, BlockPos pos, int type) {}
    private static final Map<GateKey, Long> LAST_SENT = new HashMap<>();

    private DomainParticles() {}

    /** At most one packet per tick per (position, type) — the smelting merge rule. */
    public static synchronized boolean mergedThisTick(ServerLevel level, BlockPos pos, int type) {
        return gate(level, pos, type, 1);
    }

    /** Fixed-interval limiter (acceleration 10 ticks; spawn warmup >= 5 ticks). */
    public static synchronized boolean intervalPassed(ServerLevel level, BlockPos pos, int type, int intervalTicks) {
        if (intervalTicks < 1) throw new IllegalArgumentException("Nonpositive interval");
        return gate(level, pos, type, intervalTicks);
    }

    private static boolean gate(ServerLevel level, BlockPos pos, int type, int intervalTicks) {
        long now = level.getGameTime();
        var key = new GateKey(level.dimension(), pos.immutable(), type);
        var last = LAST_SENT.get(key);
        if (last != null && now - last < intervalTicks) return false;
        LAST_SENT.put(key, now);
        // Lazy hygiene: drop gates older than a minute when the table grows.
        if (LAST_SENT.size() > 4096) {
            Iterator<Map.Entry<GateKey, Long>> iterator = LAST_SENT.entrySet().iterator();
            while (iterator.hasNext()) if (now - iterator.next().getValue() > 1200) iterator.remove();
        }
        return true;
    }

    /** Sends to every player inside {@code radius} of center; session id from the sky session counter. */
    public static void broadcast(ServerLevel level, BlockPos center, double radius, int type,
                                 @Nullable BlockPos target, long seed) {
        for (var player : level.players()) {
            if (player.blockPosition().distSqr(center) > radius * radius) continue;
            SafeDispatch.send(player, new PktDomainParticle(SignSkyService.sessionId(player), type,
                    center.asLong(), java.util.Optional.ofNullable(target).map(BlockPos::asLong), seed));
        }
    }

    /** Test/reset hook. */
    public static synchronized void resetGates() { LAST_SENT.clear(); }
}
