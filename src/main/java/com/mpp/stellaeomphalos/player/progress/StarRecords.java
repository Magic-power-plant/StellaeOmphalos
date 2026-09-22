package com.mpp.stellaeomphalos.player.progress;

import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.util.FakePlayer;

import java.util.*;

/** Server-thread facade; knowledge calls only its L0 capability. */
public final class StarRecords implements KnowledgeAccess {
    public static final StarRecords ACCESS = new StarRecords();

    private StarRecords() {}

    public static StarRecordStore store(MinecraftServer server) {
        if (!server.isSameThread())
            throw new IllegalStateException("Star record access off server thread");
        var storage = server.overworld().getDataStorage();
        // Our explicit I/O handles backup recovery before vanilla's silent read fallback.
        return stores.computeIfAbsent(
                server,
                key -> {
                    var io = new StarRecordIO(StarRecordIO.DISK);
                    var file =
                            server.getWorldPath(LevelResource.ROOT)
                                    .resolve("data")
                                    .resolve(StarRecordStore.NAME + ".dat");
                    var data = io.read(file, message -> notice(server, message));
                    var result = new StarRecordStore(data, io, message -> notice(server, message));
                    storage.set(StarRecordStore.NAME, result);
                    return result;
                });
    }

    private static final Map<MinecraftServer, StarRecordStore> stores = new IdentityHashMap<>();
    private static final Map<MinecraftServer, List<String>> notices = new IdentityHashMap<>();

    private static void notice(MinecraftServer server, String message) {
        notices.computeIfAbsent(server, ignored -> new ArrayList<>()).add(message);
        server.getPlayerList()
                .getPlayers()
                .forEach(
                        p ->
                                p.sendSystemMessage(
                                        Component.translatable(
                                                "stellaeomphalos.record." + message)));
    }

    public static void notifyLogin(ServerPlayer player) {
        notices.getOrDefault(player.server, List.of())
                .forEach(
                        message ->
                                player.sendSystemMessage(
                                        Component.translatable(
                                                "stellaeomphalos.record." + message)));
    }

    public static void stop(MinecraftServer server) {
        var store = stores.remove(server);
        if (store != null)
            store.save(
                    server.getWorldPath(LevelResource.ROOT)
                            .resolve("data")
                            .resolve(StarRecordStore.NAME + ".dat")
                            .toFile());
        notices.remove(server);
    }

    public static StarRecord get(ServerPlayer player) {
        if (player instanceof FakePlayer) return new FakeStarRecord();
        var store = store(player.server);
        boolean existed = store.contains(player.getUUID());
        var record = store.record(player.getUUID());
        if (!existed && player.getPersistentData().contains(BoonProgress.ROOT_KEY)) {
            record.captureBoons(player.getPersistentData().getCompound(BoonProgress.ROOT_KEY));
            record.reward(); // Imported pre-codex players do not receive duplicate starter books.
        }
        return record;
    }

    public static void dirty(ServerPlayer player) {
        if (!(player instanceof FakePlayer)) store(player.server).setDirty();
    }

    @Override
    public StarRecordView view(ServerPlayer player) {
        return get(player);
    }

    @Override
    public boolean promote(ServerPlayer p, StarTier tier) {
        return mutate(p, r -> r.promote(tier));
    }

    @Override
    public boolean unlockBranch(ServerPlayer p, String b) {
        return mutate(p, r -> r.branch(b));
    }

    @Override
    public boolean unlockNode(ServerPlayer p, ResourceLocation id) {
        return mutate(p, r -> r.node(id));
    }

    @Override
    public boolean collectShard(ServerPlayer p, ResourceLocation id) {
        return mutate(p, r -> r.shard(id));
    }

    @Override
    public boolean recordTarget(ServerPlayer p, ResourceLocation id) {
        return mutate(p, r -> r.target(id));
    }

    @Override
    public boolean readPage(ServerPlayer p, ResourceLocation id, String route) {
        return mutate(p, r -> r.read(id, route));
    }

    @Override
    public boolean clearRoute(ServerPlayer p) {
        return mutate(p, StarRecord::clearRoute);
    }

    public static boolean mutate(
            ServerPlayer player, java.util.function.Predicate<StarRecord> mutation) {
        boolean changed = mutation.test(get(player));
        if (changed) dirty(player);
        return changed;
    }
}
