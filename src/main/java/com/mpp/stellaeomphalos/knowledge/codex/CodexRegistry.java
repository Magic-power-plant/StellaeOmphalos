package com.mpp.stellaeomphalos.knowledge.codex;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class CodexRegistry {
    private final Map<ResourceLocation, CodexPage> defaults = new LinkedHashMap<>();
    private Map<ResourceLocation, CodexPage> pages = Map.of();
    private final Map<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();
    private final Set<ResourceLocation> warned = new HashSet<>();
    private long epoch;

    public void register(ResourceLocation id, CodexPage page, String alias) {
        var shortId = new ResourceLocation(id.getNamespace(), "codex/page/" + alias);
        if (defaults.containsKey(id) || aliases.containsKey(shortId))
            throw new IllegalArgumentException("Duplicate codex page " + id);
        defaults.put(id, page);
        aliases.put(shortId, id);
        pages = Map.copyOf(defaults);
    }

    public void replace(Map<ResourceLocation, CodexPage> entries) {
        pages = Map.copyOf(entries);
        warned.clear();
        epoch++;
    }

    public void reload(Map<ResourceLocation, CodexPage> overrides) {
        var merged = new LinkedHashMap<>(defaults);
        merged.putAll(overrides);
        replace(merged);
    }

    public Optional<CodexPage> find(ResourceLocation id) {
        id = aliases.getOrDefault(id, id);
        var page = pages.get(id);
        if (page == null && warned.add(id)) LogUtils.getLogger().warn("codex_page_missing {}", id);
        return Optional.ofNullable(page);
    }

    public Map<ResourceLocation, CodexPage> pages() {
        return pages;
    }

    public Map<ResourceLocation, CodexPage> defaults() {
        return Map.copyOf(defaults);
    }

    public long epoch() {
        return epoch;
    }
}
