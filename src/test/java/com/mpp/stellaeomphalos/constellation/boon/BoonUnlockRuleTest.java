package com.mpp.stellaeomphalos.constellation.boon;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonUnlockRuleTest {

    /** Minimal progress view; the rules under test never dereference the player parameter. */
    private static final class FakeProgress implements BoonProgressView {
        final Set<ResourceLocation> nodes = new HashSet<>();
        final Set<ResourceLocation> sealed = new HashSet<>();
        final Set<ResourceLocation> signs = new HashSet<>();
        int points;
        int level = 1;

        @Override public boolean hasNode(ResourceLocation nodeId) { return nodes.contains(nodeId); }
        @Override public boolean isSealed(ResourceLocation nodeId) { return sealed.contains(nodeId); }
        @Override public int availablePoints() { return points; }
        @Override public boolean knowsSign(ResourceLocation signId) { return signs.contains(signId); }
        @Override public int level() { return level; }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("stellaeomphalos", "test_rule/" + path);
    }

    private static BoonNode node(String path, int x, int z, UnlockRule rule) {
        return new BoonNode(id(path), BoonNodeType.NORMAL, x, z, List.of(), List.of(), rule, null);
    }

    @Test
    void standardRuleNeedsAdjacencyPointsAndPrerequisites() {
        var rule = BoonUnlockRules.standard(Set.of(id("required")));
        var a = node("a", 0, 0, rule);
        var b = node("b", 1, 0, rule);
        var graph = new BoonGraph();
        graph.connect(a, b);
        var progress = new FakeProgress();
        progress.points = 1;
        assertFalse(rule.mayUnlock(null, b, progress), "No adjacent unlocked node");
        progress.nodes.add(a.id());
        assertFalse(rule.mayUnlock(null, b, progress), "Explicit prerequisite missing");
        progress.nodes.add(id("required"));
        assertTrue(rule.mayUnlock(null, b, progress));
        progress.points = 0;
        assertFalse(rule.mayUnlock(null, b, progress), "No free skill point");
        progress.points = 1;
        progress.sealed.add(b.id());
        assertFalse(rule.mayUnlock(null, b, progress), "Sealed nodes never unlock");
    }

    @Test
    void rootRuleNeedsDiscoveredSignAndCoreRoot() {
        var sign = new ResourceLocation("stellaeomphalos", "testsign");
        var rule = BoonUnlockRules.root(sign);
        var progress = new FakeProgress();
        progress.points = 1;
        var root = new RootBoonNode(id("root"), 0, 0, sign, 1.0, List.of(), List.of(), null);
        assertFalse(rule.visible(null, progress), "Undiscovered sign hides the root entirely");
        progress.signs.add(sign);
        assertTrue(rule.visible(null, progress));
        assertFalse(rule.mayUnlock(null, root, progress), "Core root not unlocked yet");
        progress.nodes.add(BoonUnlockRules.CORE_ROOT_ID);
        assertTrue(rule.mayUnlock(null, root, progress));
        progress.points = 0;
        assertFalse(rule.mayUnlock(null, root, progress), "Roots still require a free point");
    }

    @Test
    void coreRootRuleRequiresAllMajorSignsKnown() {
        var rule = BoonUnlockRules.coreRoot();
        var progress = new FakeProgress();
        progress.points = 1;
        var core = new CoreRootBoonNode(BoonUnlockRules.CORE_ROOT_ID, 0, 0, List.of(), List.of(), null);
        // SignRegistry holds no major signs in a pure unit test, so the all-match passes vacuously.
        assertTrue(rule.mayUnlock(null, core, progress));
        progress.points = 0;
        assertFalse(rule.mayUnlock(null, core, progress));
        progress.points = 1;
        progress.sealed.add(core.id());
        assertFalse(rule.mayUnlock(null, core, progress));
    }

    @Test
    void connectorRuleUsesNeighborsOrExistingConnector() {
        var tree = new BoonTree();
        var rule = BoonUnlockRules.connector();
        var n1 = node("n1", 0, 0, rule);
        var n2 = node("n2", 1, 0, rule);
        var c1 = new ConnectorBoonNode(id("c1"), 2, 0, List.of(), List.of(), null);
        var n3 = node("n3", 4, 0, rule);
        var n4 = node("n4", 5, 0, rule);
        var c2 = new ConnectorBoonNode(id("c2"), 3, 0, List.of(), List.of(), null);
        for (var node : List.of(n1, n2, c1, n3, n4, c2)) tree.register(node);
        tree.connect(c1, n1);
        tree.connect(c1, n2);
        tree.connect(c2, n3);
        tree.connect(c2, n4);
        tree.freeze();
        BoonTree.rebuild(tree);

        var progress = new FakeProgress();
        progress.points = 5;
        assertFalse(rule.mayUnlock(null, c1, progress), "Neither all neighbors nor another connector unlocked");
        progress.nodes.add(n1.id());
        assertFalse(rule.mayUnlock(null, c1, progress), "Only one neighbor unlocked");
        progress.nodes.add(n2.id());
        assertTrue(rule.mayUnlock(null, c1, progress), "All neighbors unlocked");
        var progress2 = new FakeProgress();
        progress2.points = 5;
        progress2.nodes.add(c1.id());
        assertTrue(rule.mayUnlock(null, c2, progress2), "An unlocked connector anywhere opens the next one");
    }

    @Test
    void gatedNodesHideCompletelyBelowThreshold() {
        var gated = new GatedBoonNode(id("gated"), BoonNodeType.MAJOR, 0, 0, 5, List.of(), List.of(), Set.of(), null);
        var progress = new FakeProgress();
        progress.points = 9;
        progress.level = 4;
        assertFalse(gated.rule().visible(null, progress), "Below threshold the node is fully hidden");
        assertFalse(gated.rule().mayUnlock(null, gated, progress));
        progress.level = 5;
        assertTrue(gated.rule().visible(null, progress));
        assertFalse(gated.rule().mayUnlock(null, gated, progress), "Standard prerequisites still apply above the threshold");
        assertEquals(5, gated.minLevel());
    }

    @Test
    void connectorTokenNamesAreStable() {
        var connector = new ConnectorBoonNode(id("c"), 0, 0, List.of(), List.of(), null);
        assertEquals("connector:" + id("c") + "->" + id("n1"), connector.tokenFor(id("n1")));
    }
}
