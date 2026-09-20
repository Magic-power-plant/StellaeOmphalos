package com.mpp.stellaeomphalos.constellation.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

/**
 * Token-based natural-spawn denial service shared by Aegis (renew 1..3 ticks per tick) and Lumina
 * (renew every 80 ticks, radius 64 + mirrors x 64). A token lives at most 400 ticks without
 * renewal; a radius change rebuilds the token. Server authoritative; subscribed on the Forge bus
 * by {@link DomainBootstrap}.
 */
public final class DomainSpawnDeny {
    /** Maximum remaining lifetime a token may hold (plan: token cap 400 ticks). */
    public static final long MAX_TOKEN_TICKS = 400;

    private record Token(BlockPos center, double radius, long expiresAt) {}
    private record ZoneKey(ResourceKey<Level> dimension, BlockPos center) {}

    private static final Map<ZoneKey, Token> TOKENS = new LinkedHashMap<>();

    private DomainSpawnDeny() {}

    /** Renews the token at center, extending its lifetime by extraTicks but never past the 400-tick cap. */
    public static synchronized void renew(ServerLevel level, BlockPos center, double radius, long extraTicks) {
        if (extraTicks < 1) throw new IllegalArgumentException("Nonpositive renewal");
        var key = new ZoneKey(level.dimension(), center.immutable());
        long now = level.getGameTime();
        var existing = TOKENS.get(key);
        if (existing != null && existing.radius() == radius) {
            long next = Math.min(existing.expiresAt() + extraTicks, now + MAX_TOKEN_TICKS);
            TOKENS.put(key, new Token(existing.center(), radius, next));
        } else {
            // Radius changed or new zone: rebuild the token.
            TOKENS.put(key, new Token(center.immutable(), radius, now + Math.min(extraTicks, MAX_TOKEN_TICKS)));
        }
    }

    /** Removes the token at center (structure teardown). */
    public static synchronized void revoke(ServerLevel level, BlockPos center) {
        TOKENS.remove(new ZoneKey(level.dimension(), center));
    }

    /** Whether a natural spawn at pos is denied right now. */
    public static synchronized boolean denies(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        var iterator = TOKENS.entrySet().iterator();
        boolean denied = false;
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var token = entry.getValue();
            if (token.expiresAt() <= now) { iterator.remove(); continue; }
            if (!entry.getKey().dimension().equals(level.dimension())) continue;
            if (token.center().distSqr(pos) <= token.radius() * token.radius()) denied = true;
        }
        return denied;
    }

    static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL || event.isSpawnCancelled()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (denies(level, event.getEntity().blockPosition())) event.setSpawnCancelled(true);
    }

    /** Drops all tokens (server stop / tests). */
    public static synchronized void clear() { TOKENS.clear(); }

    /** Visible for tests. */
    public static synchronized int tokenCount() { return TOKENS.size(); }
}
