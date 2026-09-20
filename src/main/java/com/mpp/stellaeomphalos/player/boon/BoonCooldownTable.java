package com.mpp.stellaeomphalos.player.boon;

import com.mpp.stellaeomphalos.core.util.CooldownGuard;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

/**
 * In-memory cooldown table for boon actions (unlock command spam guard and future GUI actions).
 * Backed by {@link CooldownGuard}; session-scoped, never persisted.
 */
public final class BoonCooldownTable {

    private static final BoonCooldownTable INSTANCE = new BoonCooldownTable();

    private final CooldownGuard guard = new CooldownGuard();
    private long now;

    private BoonCooldownTable() {}

    public static BoonCooldownTable get() {
        return INSTANCE;
    }

    public void tick(long serverTick) {
        now = serverTick;
        if (serverTick % 200 == 0) guard.expire(serverTick);
    }

    /** @return true when the action is off cooldown and the cooldown is now armed. */
    public boolean acquire(UUID player, ResourceLocation ability, long durationTicks) {
        return guard.acquire(player, ability, now, durationTicks);
    }

    public void remove(UUID player) {
        guard.remove(player);
    }

    public void clear() {
        guard.clear();
    }
}
