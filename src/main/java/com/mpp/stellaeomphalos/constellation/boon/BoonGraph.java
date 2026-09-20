package com.mpp.stellaeomphalos.constellation.boon;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Symmetric adjacency table plus the ordered, de-duplicated render edge list.
 * Invariants: every edge a→b implies b→a; render edges hold each undirected pair exactly once.
 */
public final class BoonGraph {

    private final Map<ResourceLocation, BoonNode> participants = new LinkedHashMap<>();
    private final Set<BoonEdge> edges = new LinkedHashSet<>();

    /** Connects two nodes symmetrically and records the render edge. */
    public void connect(BoonNode a, BoonNode b) {
        if (a == null || b == null) throw new IllegalArgumentException("null node");
        if (a == b) throw new IllegalArgumentException("Self connection at " + a.id());
        participants.putIfAbsent(a.id(), a);
        participants.putIfAbsent(b.id(), b);
        a.link(b);
        b.link(a);
        edges.add(new BoonEdge(a.id(), b.id()));
    }

    public void disconnect(BoonNode a, BoonNode b) {
        a.unlink(b);
        b.unlink(a);
        edges.remove(new BoonEdge(a.id(), b.id()));
    }

    /** Removes every edge touching the node. The neighbor set is copied first to avoid CME. */
    public boolean disconnectAll(BoonNode node) {
        var neighbors = List.copyOf(node.linkView());
        boolean removed = false;
        for (var neighborId : neighbors) {
            var neighbor = participants.get(neighborId);
            if (neighbor == null) continue;
            node.unlink(neighbor);
            neighbor.unlink(node);
            removed = true;
        }
        edges.removeIf(edge -> edge.touches(node.id()));
        return removed;
    }

    public Set<ResourceLocation> neighbors(BoonNode node) {
        return node.neighbors();
    }

    /** Ordered, de-duplicated edges for client rendering; direction is irrelevant. */
    public List<BoonEdge> renderEdges() {
        return List.copyOf(edges);
    }
}
