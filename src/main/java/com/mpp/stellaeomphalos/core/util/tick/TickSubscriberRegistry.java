package com.mpp.stellaeomphalos.core.util.tick;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Explicit, removable subscriptions; dispatch uses a snapshot to tolerate reentrant removal. */
public final class TickSubscriberRegistry implements AutoCloseable {
    public enum Phase { SERVER_END, CLIENT_END, LEVEL_END, PLAYER_END }
    public record Context(Phase phase, long tick, Object source) {}
    private final Map<Phase, Map<String, Consumer<Context>>> subscribers = new EnumMap<>(Phase.class);
    public AutoCloseable subscribe(Phase phase, String id, Consumer<Context> consumer) {
        var entries = subscribers.computeIfAbsent(phase, ignored -> new LinkedHashMap<>());
        if (entries.putIfAbsent(id, consumer) != null) throw new IllegalArgumentException("Duplicate subscriber " + id);
        return () -> entries.remove(id, consumer);
    }
    public void dispatch(Context context) {
        for (var consumer : java.util.List.copyOf(subscribers.getOrDefault(context.phase(), Map.of()).values())) consumer.accept(context);
    }
    @Override public void close() { subscribers.clear(); }
}
