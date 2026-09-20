package com.mpp.stellaeomphalos.player.boon.root;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.constellation.boon.RootBoonNode;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shared plumbing for the five root behavior hooks: checks whether the player unlocked the
 * sign's root node, applies the root's experience multiplier, and accumulates fractional
 * experience so small trickles are not lost to rounding. Session-scoped; cleared on logout.
 */
final class RootExpSupport {

    private static final Map<UUID, Map<String, Double>> REMAINDERS = new HashMap<>();

    private RootExpSupport() {}

    static ResourceLocation rootId(String sign) {
        return new ResourceLocation(Omphalos.MODID, sign + "/root");
    }

    /** True when the player has unlocked the sign's root node. */
    static boolean active(ServerPlayer player, String sign) {
        if (!BoonTree.ready()) return false;
        return BoonProgress.getServer(player).hasNode(rootId(sign));
    }

    static double multiplier(String sign) {
        if (!BoonTree.ready()) return 1.0;
        return BoonTree.get().node(rootId(sign)) instanceof RootBoonNode root ? root.expMultiplier() : 1.0;
    }

    /** Grants fractional experience: whole units go to the progress store, the rest carries over. */
    static void grant(ServerPlayer player, String sign, double amount) {
        if (amount <= 0 || !Double.isFinite(amount)) return;
        var progress = BoonProgress.getServer(player);
        double total = amount * multiplier(sign) + remainder(player.getUUID(), sign);
        long whole = (long) total;
        REMAINDERS.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(sign, total - whole);
        if (whole > 0) progress.grantExp(player, whole);
    }

    private static double remainder(UUID player, String sign) {
        return REMAINDERS.getOrDefault(player, Map.of()).getOrDefault(sign, 0.0);
    }

    static void clear(UUID player) {
        REMAINDERS.remove(player);
    }

    static void clearAll() {
        REMAINDERS.clear();
    }
}
