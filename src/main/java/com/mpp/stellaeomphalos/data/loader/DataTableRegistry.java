package com.mpp.stellaeomphalos.data.loader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Declarations freeze before reload; a complete successful reload publishes one snapshot. */
public final class DataTableRegistry {
    private final Map<ResourceLocation, DataTable<?>> declarations = new LinkedHashMap<>();
    private volatile Map<ResourceLocation, Map<ResourceLocation, ?>> snapshot = Map.of();
    private volatile List<DataLoadReport.Issue> report = List.of();
    private boolean frozen;
    public <T> DataTable<T> declare(DataTable<T> table) {
        if (frozen || declarations.putIfAbsent(table.id(), table) != null) throw new IllegalStateException("Late or duplicate table " + table.id());
        return table;
    }
    public List<DataTable<?>> tables() { frozen = true; return List.copyOf(declarations.values()); }
    @SuppressWarnings("unchecked")
    public <T> Map<ResourceLocation, T> entries(DataTable<T> table) {
        if (declarations.get(table.id()) != table) throw new IllegalArgumentException("Undeclared table");
        return (Map<ResourceLocation, T>) snapshot.getOrDefault(table.id(), Map.of());
    }
    public void publish(Map<ResourceLocation, Map<ResourceLocation, ?>> candidate, DataLoadReport result) {
        report = result.issues();
        if (com.mpp.stellaeomphalos.OmphalosConfig.COMMON.flag("logging.dataTableVerbose"))
            com.mojang.logging.LogUtils.getLogger().info("Data reload: {} tables, {} issues, accepted={}",
                    candidate.size(), report.size(), !result.hasErrors());
        if (!result.hasErrors()) {
            var copy = new LinkedHashMap<ResourceLocation, Map<ResourceLocation, ?>>();
            candidate.forEach((key, values) -> copy.put(key, Map.copyOf(values)));
            snapshot = Map.copyOf(copy);
        }
    }
    public List<DataLoadReport.Issue> report() { return report; }
    public void clearSession() { snapshot = Map.of(); report = List.of(); }
}
