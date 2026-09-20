package com.mpp.stellaeomphalos.player.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonValueBridge;
import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.network.SafeDispatch;
import com.mpp.stellaeomphalos.network.toClient.PktBoonDelta;
import com.mpp.stellaeomphalos.network.toClient.PktBoonExp;
import com.mpp.stellaeomphalos.network.toClient.PktBoonTreeSync;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side lifecycle and sync dispatcher for boon progress: login/respawn full syncs,
 * per-node deltas, experience packets batched to at most one per tick per player, and the death
 * penalty (25% of the current level band). Sessions increment per login so client mirrors drop
 * late packets from previous sessions.
 */
public final class BoonEffectDispatcher {

    private static final Map<UUID, Integer> SESSIONS = new HashMap<>();
    private static final Set<UUID> EXP_DIRTY = new LinkedHashSet<>();
    private static final Map<UUID, PktBoonExp> LAST_SENT = new HashMap<>();

    private BoonEffectDispatcher() {}

    static void onLogin(ServerPlayer player) {
        SESSIONS.merge(player.getUUID(), 1, Integer::sum);
        BoonProgress.getServer(player);
        sendTreeSync(player);
        markExpDirty(player);
    }

    static void onRespawn(ServerPlayer player) {
        sendTreeSync(player);
        markExpDirty(player);
    }

    static void onLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        BoonProgress.dropCache(id);
        SESSIONS.remove(id);
        EXP_DIRTY.remove(id);
        LAST_SENT.remove(id);
        BoonCooldownTable.get().remove(id);
        BoonValueBridge.invalidate(id);
    }

    static void onDeath(ServerPlayer player) {
        BoonProgress.getServer(player).applyDeathPenalty(player);
    }

    static void onServerTick(MinecraftServer server, long tick) {
        BoonCooldownTable.get().tick(tick);
        if (EXP_DIRTY.isEmpty()) return;
        var dirty = Set.copyOf(EXP_DIRTY);
        EXP_DIRTY.clear();
        for (var id : dirty) {
            var player = server.getPlayerList().getPlayer(id);
            if (player == null) continue;
            var progress = BoonProgress.getServer(player);
            var packet = new PktBoonExp(sessionOf(id), progress.exp(), progress.level(),
                    Math.max(0, progress.availablePoints()));
            if (packet.equals(LAST_SENT.get(id))) continue;    // value-changed-only sending
            LAST_SENT.put(id, packet);
            SafeDispatch.send(player, packet);
        }
    }

    static void onServerStop() {
        SESSIONS.clear();
        EXP_DIRTY.clear();
        LAST_SENT.clear();
        BoonCooldownTable.get().clear();
    }

    /** Queues an experience/points sync for the player; flushed once per server tick. */
    public static void markExpDirty(ServerPlayer player) {
        EXP_DIRTY.add(player.getUUID());
    }

    public static void sendDelta(ServerPlayer player, ResourceLocation nodeId, byte action, CompoundTag extra) {
        SafeDispatch.send(player, new PktBoonDelta(sessionOf(player.getUUID()), nodeId, action, extra));
    }

    /** Full tree sync: layout for every node plus the player's applied bitmap. */
    public static void sendTreeSync(ServerPlayer player) {
        if (!BoonTree.ready()) return;
        var tree = BoonTree.get();
        var progress = BoonProgress.getServer(player);
        var entries = tree.nodes().stream()
                .map(node -> new PktBoonTreeSync.Entry(node.id(), (short) node.gridX(), (short) node.gridZ(),
                        (byte) node.type().ordinal(), progress.hasNode(node.id())))
                .toList();
        SafeDispatch.send(player, new PktBoonTreeSync(sessionOf(player.getUUID()), BoonTree.BOON_TREE_VERSION, entries));
    }

    private static int sessionOf(UUID playerId) {
        return SESSIONS.getOrDefault(playerId, 0);
    }
}
