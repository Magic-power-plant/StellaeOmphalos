package com.mpp.stellaeomphalos.knowledge.research;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class StudyNodeRegistry {
    private final Map<ResourceLocation, StudyNode> nodes = new LinkedHashMap<>();

    public void register(StudyNode node) {
        if (nodes.containsKey(node.id())
                || nodes.values().stream()
                        .anyMatch(
                                n ->
                                        n.branch() == node.branch()
                                                && n.x() == node.x()
                                                && n.y() == node.y()))
            throw new IllegalArgumentException("Duplicate study id or coordinates: " + node.id());
        nodes.put(node.id(), node);
    }

    public void validate() {
        for (var id : nodes.keySet()) visit(id, new HashSet<>(), new HashSet<>());
    }

    private void visit(
            ResourceLocation id, Set<ResourceLocation> visiting, Set<ResourceLocation> done) {
        if (done.contains(id)) return;
        if (!visiting.add(id)) throw new IllegalArgumentException("Study cycle at " + id);
        var node = nodes.get(id);
        if (node == null) throw new IllegalArgumentException("Missing prerequisite " + id);
        node.prerequisites().forEach(p -> visit(p, visiting, done));
        visiting.remove(id);
        done.add(id);
    }

    public Optional<StudyNode> find(ResourceLocation id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public Collection<StudyNode> all() {
        return List.copyOf(nodes.values());
    }
}
