package com.mpp.stellaeomphalos.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class CooldownGuard {
    private record Key(UUID player, ResourceLocation ability) {}
    private final Map<Key, Long> deadlines = new HashMap<>();
    public boolean acquire(UUID player, ResourceLocation ability, long now, long duration) {
        if (duration < 1) throw new IllegalArgumentException("Nonpositive cooldown");
        var key = new Key(player, ability);
        if (deadlines.getOrDefault(key, Long.MIN_VALUE) > now) return false;
        deadlines.put(key, Math.addExact(now, duration)); return true;
    }
    public void expire(long now) { deadlines.values().removeIf(deadline -> deadline <= now); }
    public void remove(UUID player) { deadlines.keySet().removeIf(key -> key.player.equals(player)); }
    public void clear() { deadlines.clear(); }
}
