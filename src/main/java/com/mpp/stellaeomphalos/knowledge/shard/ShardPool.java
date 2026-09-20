package com.mpp.stellaeomphalos.knowledge.shard;

import com.mpp.stellaeomphalos.knowledge.research.GateContext;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Predicate;

public final class ShardPool {
    private final Map<ResourceLocation, LoreShard> entries = new LinkedHashMap<>();

    public void register(LoreShard shard) {
        if (entries.putIfAbsent(shard.id(), shard) != null)
            throw new IllegalArgumentException("Duplicate lore shard " + shard.id());
    }

    public List<LoreShard> eligible(GateContext context, Predicate<String> localized) {
        return entries.values().stream()
                .filter(s -> s.discoverable(context) && s.localized(localized))
                .sorted(Comparator.comparing(s -> s.id().toString()))
                .toList();
    }

    /**
     * Selection order depends only on ids and the item's seed, never locale or registration order.
     */
    public static Optional<LoreShard> resolve(long seed, List<LoreShard> pool) {
        if (pool.isEmpty()) return Optional.empty();
        var sorted = pool.stream().sorted(Comparator.comparing(s -> s.id().toString())).toList();
        return Optional.of(sorted.get(new java.util.SplittableRandom(seed).nextInt(sorted.size())));
    }

    public Optional<LoreShard> find(ResourceLocation id) {
        return Optional.ofNullable(entries.get(id));
    }

    public List<LoreShard> all() {
        return List.copyOf(entries.values());
    }
}
