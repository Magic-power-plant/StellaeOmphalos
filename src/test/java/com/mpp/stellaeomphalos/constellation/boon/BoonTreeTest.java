package com.mpp.stellaeomphalos.constellation.boon;

import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonTreeTest {

    private static ResourceLocation id(String path) {
        return new ResourceLocation("stellaeomphalos", "test_tree/" + path);
    }

    private static BoonNode node(String path, int x, int z) {
        return new BoonNode(id(path), BoonNodeType.NORMAL, x, z, List.of(), List.of(),
                BoonUnlockRules.standard(Set.of()), null);
    }

    @Test
    void duplicatePositionFailsFast() {   // AC-2.15
        var tree = new BoonTree();
        tree.register(node("a", 3, 3));
        assertThrows(IllegalStateException.class, () -> tree.register(node("b", 3, 3)),
                "Reusing a grid position must throw instead of silently overwriting");
    }

    @Test
    void duplicateIdFailsFast() {
        var tree = new BoonTree();
        tree.register(node("a", 0, 0));
        assertThrows(IllegalStateException.class, () -> tree.register(node("a", 5, 5)));
    }

    @Test
    void freezeIsIrreversible() {
        var tree = new BoonTree();
        var a = node("a", 0, 0);
        var b = node("b", 1, 0);
        tree.register(a);
        tree.register(b);
        tree.freeze();
        assertThrows(IllegalStateException.class, () -> tree.register(node("c", 2, 0)));
        assertThrows(IllegalStateException.class, () -> tree.connect(a, b));
        assertThrows(IllegalStateException.class, () -> a.link(b), "Node links seal with the tree");
    }

    @Test
    void lookupRootsAndRenderEdges() {
        var tree = new BoonTree();
        var root = new RootBoonNode(id("sign/root"), 0, 0, new ResourceLocation("stellaeomphalos", "testsign"),
                1.0, List.of(), List.of(), null);
        var child = node("sign/child", 1, 0);
        tree.registerRoot(new ResourceLocation("stellaeomphalos", "testsign"), root);
        tree.register(child);
        tree.connect(root, child);
        tree.connect(child, root);   // duplicate undirected edge collapses
        tree.freeze();
        assertSame(root, tree.node(id("sign/root")));
        assertNull(tree.node(id("sign/missing")));
        assertSame(root, tree.rootOf(new ResourceLocation("stellaeomphalos", "testsign")));
        assertEquals(2, tree.nodes().size());
        assertEquals(1, tree.renderEdges().size());
        var edge = tree.renderEdges().get(0);
        assertTrue(edge.touches(root.id()) && edge.touches(child.id()));
        assertThrows(IllegalStateException.class, () -> tree.registerRoot(
                new ResourceLocation("stellaeomphalos", "testsign"), root), "Duplicate root per sign rejected");
    }

    @Test
    void rebuildSwapsTheActiveInstance() {
        var first = new BoonTree();
        first.register(node("first", 0, 0));
        first.freeze();
        BoonTree.rebuild(first);
        assertSame(first, BoonTree.get());
        var second = new BoonTree();
        second.register(node("second", 9, 9));
        second.freeze();
        BoonTree.rebuild(second);
        assertSame(second, BoonTree.get());
        assertNotNull(BoonTree.get().node(id("second")));
        assertNull(BoonTree.get().node(id("first")), "Old tree contents must not leak across rebuild");
        var unfrozen = new BoonTree();
        assertThrows(IllegalArgumentException.class, () -> BoonTree.rebuild(unfrozen));
    }
}
