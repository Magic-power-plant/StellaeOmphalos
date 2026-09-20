package com.mpp.stellaeomphalos.lumen.transport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Registry for non-block lumen receivers (floating crystals, loaded minecarts, ...).
 * Registration happens during common setup; the registry freezes afterwards. Duplicates throw.
 */
public final class LumenSinkRegistry {
    private static final Map<ResourceLocation, LumenSinkFactory> BY_ID = new LinkedHashMap<>();
    private static final Map<EntityType<?>, LumenSinkFactory> BY_ENTITY = new LinkedHashMap<>();
    private static final Map<Item, LumenSinkFactory> BY_ITEM = new LinkedHashMap<>();
    private static boolean frozen;
    private LumenSinkRegistry() {}

    public static synchronized void register(ResourceLocation id, LumenSinkFactory factory) {
        if (frozen) throw new IllegalStateException("Lumen sink registry frozen");
        if (BY_ID.putIfAbsent(id, factory) != null) throw new IllegalArgumentException("Duplicate lumen sink " + id);
    }
    public static synchronized void registerEntity(EntityType<?> type, LumenSinkFactory factory) {
        if (frozen) throw new IllegalStateException("Lumen sink registry frozen");
        if (BY_ENTITY.putIfAbsent(type, factory) != null) throw new IllegalArgumentException("Duplicate lumen sink entity " + type);
    }
    public static synchronized void registerItem(Item item, LumenSinkFactory factory) {
        if (frozen) throw new IllegalStateException("Lumen sink registry frozen");
        if (BY_ITEM.putIfAbsent(item, factory) != null) throw new IllegalArgumentException("Duplicate lumen sink item " + item);
    }
    public static Optional<LumenSinkFactory> byId(ResourceLocation id) { return Optional.ofNullable(BY_ID.get(id)); }
    public static Optional<LumenSinkFactory> byEntity(EntityType<?> type) { return Optional.ofNullable(BY_ENTITY.get(type)); }
    public static Optional<LumenSinkFactory> byItem(Item item) { return Optional.ofNullable(BY_ITEM.get(item)); }
    public static synchronized void freeze() { frozen = true; }
}
