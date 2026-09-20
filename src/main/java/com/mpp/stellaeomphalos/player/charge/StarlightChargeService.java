package com.mpp.stellaeomphalos.player.charge;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.network.SafeDispatch;
import com.mpp.stellaeomphalos.network.toClient.PktChargeSync;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-authoritative starlight charge service. Lifecycle: created lazily per server and
 * dropped on ServerStoppingEvent (ChargeBootstrap); listeners live on the Forge bus.
 *
 * dayFactor interpretation: 1.0 while the level reports day, 0.0 at night, so the regen
 * multiplier is base*1.0 by day and base*0.5 by night, times 6 under open sky.
 */
public final class StarlightChargeService {
    private static StarlightChargeService current;
    private final MinecraftServer server;
    private final ChargeLedger ledger = new ChargeLedger();
    private final Map<UUID, Integer> sessions = new HashMap<>();

    private StarlightChargeService(MinecraftServer server) { this.server = server; }

    public static StarlightChargeService get(MinecraftServer server) {
        if (current == null || current.server != server) current = new StarlightChargeService(server);
        return current;
    }

    static void shutdown() { current = null; }

    /** Per-player sync session: bumped on every login so stale client mirrors drop late packets. */
    void bumpSession(UUID playerId) { sessions.merge(playerId, 1, Integer::sum); }

    private int sessionOf(UUID playerId) { return sessions.getOrDefault(playerId, 0); }

    public float charge(ServerPlayer player) {
        UUID id = player.getUUID();
        boolean fresh = !ledger.contains(id);
        float value = ledger.charge(id);
        if (player.isCreative()) value = ledger.set(id, ChargeLedger.MAX_CHARGE);
        if (fresh) {
            // First access initializes and synchronizes a full charge (contract 2.6).
            sync(player, value);
            ledger.markSynced(id, value);
        }
        return value;
    }

    public boolean hasAtLeast(ServerPlayer player, float amount) {
        return player.isCreative() || charge(player) >= amount;
    }

    /** @param simulate true only probes; no value is deducted. */
    public boolean drain(ServerPlayer player, float amount, boolean simulate) {
        if (player.isCreative()) return true;
        return ledger.drain(player.getUUID(), amount, simulate);
    }

    public void onDisconnect(UUID playerId) {
        ledger.remove(playerId);
        sessions.remove(playerId);
    }

    void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        float current = charge(player);
        var level = player.serverLevel();
        float dayFactor = level.isDay() ? 1.0F : 0.0F;
        boolean canSeeSky = level.canSeeSky(player.blockPosition());
        float next = ChargeLedger.tickValue(current,
                (float) OmphalosConfig.SERVER.decimal("gameplay.chargeRegenPerTick"), dayFactor, canSeeSky, player.isCreative());
        if (next != current) ledger.set(id, next);
        if (ledger.shouldSync(id, next)) {
            sync(player, next);
            ledger.markSynced(id, next);
        }
    }

    private void sync(ServerPlayer player, float value) {
        SafeDispatch.send(player, new PktChargeSync(sessionOf(player.getUUID()), ChargeLedger.quantize(value)));
    }
}
