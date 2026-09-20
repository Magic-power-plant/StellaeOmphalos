package com.mpp.stellaeomphalos.knowledge.codex;

import com.mpp.stellaeomphalos.knowledge.research.*;

import net.minecraft.world.item.*;

import java.util.*;

public final class CodexLookupIndex {
    private record Entry(ItemStack exemplar, boolean exact, CodexRoute target) {}

    private final Map<Item, List<Entry>> buckets = new HashMap<>();

    public void clear() {
        buckets.clear();
    }

    public void register(ItemStack exemplar, boolean exact, CodexRoute target) {
        buckets.computeIfAbsent(exemplar.getItem(), key -> new ArrayList<>())
                .add(new Entry(exemplar.copy(), exact, target));
    }

    public Optional<CodexRoute> find(
            ItemStack stack, StudyNodeRegistry nodes, GateContext context) {
        return buckets.getOrDefault(stack.getItem(), List.of()).stream()
                .filter(e -> !e.exact() || ItemStack.isSameItemSameTags(e.exemplar(), stack))
                .map(Entry::target)
                .filter(
                        r ->
                                nodes.find(r.node())
                                        .map(n -> n.visibility(context).level().readable())
                                        .orElse(false))
                .findFirst();
    }
}
