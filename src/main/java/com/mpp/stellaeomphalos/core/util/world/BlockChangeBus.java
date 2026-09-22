package com.mpp.stellaeomphalos.core.util.world;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Server-only block change fanout with explicit source information and idempotent subscriptions. */
public final class BlockChangeBus {
    public enum ChangeSource { PLAYER, ENTITY, WORLDGEN, COMMAND, EXPLOSION, FLUID, MACHINE, UNKNOWN }
    @FunctionalInterface public interface Subscriber {
        void onChanged(ServerLevel level, BlockPos pos, BlockState before, BlockState after, ChangeSource source);
    }

    private static final MapHolder INSTANCES = new MapHolder();
    private final Set<Subscriber> subscribers = new LinkedHashSet<>();

    private BlockChangeBus() {}

    public static BlockChangeBus get(ServerLevel level) { return INSTANCES.get(level); }
    public boolean subscribe(Subscriber subscriber) { return subscribers.add(subscriber); }
    public boolean unsubscribe(Subscriber subscriber) { return subscribers.remove(subscriber); }
    public void clear() { subscribers.clear(); }

    public static void publish(ServerLevel level, BlockPos pos, BlockState before, BlockState after,
                               ChangeSource source) {
        for (var subscriber : java.util.List.copyOf(get(level).subscribers))
            subscriber.onChanged(level, pos.immutable(), before, after, source);
    }

    public static void unload(ServerLevel level) { INSTANCES.remove(level); }

    private static final class MapHolder {
        private final IdentityHashMap<ServerLevel, BlockChangeBus> values = new IdentityHashMap<>();
        synchronized BlockChangeBus get(ServerLevel level) { return values.computeIfAbsent(level, ignored -> new BlockChangeBus()); }
        synchronized void remove(ServerLevel level) { var bus = values.remove(level); if (bus != null) bus.clear(); }
    }
}
