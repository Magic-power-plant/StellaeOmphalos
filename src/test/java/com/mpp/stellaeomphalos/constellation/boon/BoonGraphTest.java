package com.mpp.stellaeomphalos.constellation.boon;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonGraphTest {

    private static BoonNode node(String path) {
        return new BoonNode(new ResourceLocation("stellaeomphalos", "test_graph/" + path),
                BoonNodeType.NORMAL, 0, 0, List.of(), List.of(), BoonUnlockRules.standard(Set.of()), null);
    }

    @Test
    void connectIsSymmetricAndEdgesAreDeduped() {
        var graph = new BoonGraph();
        var a = node("a");
        var b = node("b");
        graph.connect(a, b);
        assertTrue(a.neighbors().contains(b.id()) && b.neighbors().contains(a.id()), "Adjacency must be symmetric");
        graph.connect(b, a);
        assertEquals(1, graph.renderEdges().size(), "Render edges dedupe the undirected pair");
        graph.disconnect(a, b);
        assertTrue(a.neighbors().isEmpty() && b.neighbors().isEmpty());
        assertTrue(graph.renderEdges().isEmpty());
    }

    @Test
    void selfConnectionRejected() {
        var graph = new BoonGraph();
        var a = node("a");
        assertThrows(IllegalArgumentException.class, () -> graph.connect(a, a));
    }

    @Test
    void disconnectAllCopiesNeighborsBeforeMutating() {
        var graph = new BoonGraph();
        var center = node("center");
        var leaves = List.of(node("l1"), node("l2"), node("l3"));
        leaves.forEach(leaf -> graph.connect(center, leaf));
        // Disconnecting while the adjacency set feeds the iteration must not throw CME.
        assertTrue(graph.disconnectAll(center));
        assertTrue(center.neighbors().isEmpty());
        for (var leaf : leaves) assertFalse(leaf.neighbors().contains(center.id()));
        assertTrue(graph.renderEdges().isEmpty());
        assertFalse(graph.disconnectAll(center), "Second disconnectAll reports nothing removed");
    }
}
