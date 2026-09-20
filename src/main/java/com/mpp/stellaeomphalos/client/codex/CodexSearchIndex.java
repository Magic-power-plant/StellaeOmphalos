package com.mpp.stellaeomphalos.client.codex;

import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.knowledge.research.*;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;

public final class CodexSearchIndex {
    public record Result(
            CodexRoute route,
            String title,
            String text,
            StudyBranch branch,
            ResourceLocation page) {}

    private List<Result> entries = List.of();

    public void build(
            StudyNodeRegistry nodes,
            Map<ResourceLocation, CodexPage> pages,
            GateContext context,
            Function<String, String> translate,
            Function<ResourceLocation, String> itemName) {
        var next = new ArrayList<Result>();
        for (var node : nodes.all())
            if (node.visibility(context).level().readable())
                for (int i = 0; i < node.pages().size(); i++) {
                    var id = node.pages().get(i);
                    var page = pages.get(id);
                    if (page == null || !page.visibleWhen().evaluate(context).level().readable())
                        continue;
                    String title = translate.apply(page.title());
                    next.add(
                            new Result(
                                    new CodexRoute(node.id(), i),
                                    title,
                                    (title
                                                    + " "
                                                    + translate.apply(page.body())
                                                    + " "
                                                    + itemName.apply(node.icon()))
                                            .toLowerCase(Locale.ROOT),
                                    node.branch(),
                                    id));
                }
        entries = List.copyOf(next);
    }

    public List<Result> search(
            String query, String branch, boolean unread, Map<ResourceLocation, Integer> seen) {
        String normalized = query.toLowerCase(Locale.ROOT);
        return entries.stream()
                .filter(
                        e ->
                                e.text().contains(normalized)
                                        && (branch.isEmpty() || e.branch().name().equals(branch))
                                        && (!unread || seen.getOrDefault(e.page(), 0) == 0))
                .toList();
    }

    public void clear() {
        entries = List.of();
    }
}
