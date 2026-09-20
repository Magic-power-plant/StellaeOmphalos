package com.mpp.stellaeomphalos.client.codex;

import com.mojang.logging.LogUtils;

import java.util.*;

public final class CodexRibbonRegistry {
    public record Ribbon(int order, String name) {}

    private final SortedMap<Integer, Ribbon> ribbons = new TreeMap<>();

    public void register(Ribbon ribbon) {
        if (ribbons.putIfAbsent(ribbon.order(), ribbon) != null) {
            LogUtils.getLogger().error("Duplicate codex ribbon order {}", ribbon.order());
            throw new IllegalArgumentException("Duplicate ribbon order");
        }
    }

    public List<Ribbon> all() {
        return List.copyOf(ribbons.values());
    }

    public static CodexRibbonRegistry defaults() {
        var result = new CodexRibbonRegistry();
        int order = 0;
        for (String name : List.of("study", "signs", "boons", "lore", "milestones", "search"))
            result.register(new Ribbon(order += 10, name));
        return result;
    }
}
