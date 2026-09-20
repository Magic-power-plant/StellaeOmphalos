package com.mpp.stellaeomphalos.player.charge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure UUID-keyed charge ledger; free of Minecraft types so JUnit can exercise it without an instance.
 * Server-authoritative; clients only ever see the quantized mirror.
 */
public final class ChargeLedger {
    public static final float MAX_CHARGE = 1.0F;
    public static final int QUANTUM = 256;
    private final Map<UUID, Float> charges = new HashMap<>();
    private final Map<UUID, Integer> lastSync = new HashMap<>();

    public boolean contains(UUID playerId) { return charges.containsKey(playerId); }

    /** First access initializes the account to full charge ("new players start full" is deliberate). */
    public float charge(UUID playerId) { return charges.computeIfAbsent(playerId, key -> MAX_CHARGE); }

    public float set(UUID playerId, float value) {
        float clamped = Math.max(0, Math.min(MAX_CHARGE, value));
        charges.put(playerId, clamped);
        return clamped;
    }

    public boolean drain(UUID playerId, float amount, boolean simulate) {
        if (amount < 0 || Float.isNaN(amount)) throw new IllegalArgumentException("Invalid drain amount " + amount);
        float current = charge(playerId);
        if (current < amount) return false;
        if (!simulate) charges.put(playerId, current - amount);
        return true;
    }

    public static int quantize(float charge) {
        return Math.max(0, Math.min(QUANTUM, Math.round(charge * QUANTUM)));
    }

    /** Sync only while below full and only when the quantized value actually moved. */
    public boolean shouldSync(UUID playerId, float charge) {
        return charge < MAX_CHARGE && quantize(charge) != lastSync.getOrDefault(playerId, -1);
    }

    public void markSynced(UUID playerId, float charge) { lastSync.put(playerId, quantize(charge)); }

    public void remove(UUID playerId) { charges.remove(playerId); lastSync.remove(playerId); }

    public void clear() { charges.clear(); lastSync.clear(); }

    /** Regen formula (contract 2.6): base * (0.5 + dayFactor*0.5) * (sky ? 6 : 1). */
    public static float regenGain(float base, float dayFactor, boolean canSeeSky) {
        return base * (0.5F + dayFactor * 0.5F) * (canSeeSky ? 6 : 1);
    }

    /** Creative players are pinned to full and skip regen math entirely. */
    public static float tickValue(float current, float base, float dayFactor, boolean canSeeSky, boolean creative) {
        if (creative) return MAX_CHARGE;
        return Math.min(MAX_CHARGE, current + regenGain(base, dayFactor, canSeeSky));
    }
}
