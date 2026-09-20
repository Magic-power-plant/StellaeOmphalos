package com.mpp.stellaeomphalos.player.boon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonLedgerTest {

    private static ResourceLocation id(String path) {
        return new ResourceLocation("stellaeomphalos", "test_ledger/" + path);
    }

    @Test
    void commitRemovesBeforeAdditions() {
        var ledger = new BoonLedger();
        var order = new ArrayList<String>();
        var ledgerWriter = new BoonLedger.Writer() {
            @Override public void add(ResourceLocation nodeId) { order.add("+" + nodeId.getPath()); }
            @Override public void remove(ResourceLocation nodeId) { order.add("-" + nodeId.getPath()); }
        };
        ledger.scheduleAdd(id("a"));
        ledger.scheduleRemove(id("b"));
        ledger.scheduleAdd(id("c"));
        ledger.commit(ledgerWriter);
        assertEquals(List.of("-test_ledger/b", "+test_ledger/a", "+test_ledger/c"), order,
                "Removals run before additions (先拆后装)");
        assertTrue(ledger.isEmpty());
    }

    @Test
    void latestIntentWinsPerNode() {
        var ledger = new BoonLedger();
        ledger.scheduleAdd(id("a"));
        ledger.scheduleRemove(id("a"));
        ledger.scheduleRemove(id("b"));
        ledger.scheduleAdd(id("b"));
        var applied = new LinkedHashSet<ResourceLocation>();
        applied.add(id("a"));
        ledger.commit(new BoonLedger.Writer() {
            @Override public void add(ResourceLocation nodeId) { applied.add(nodeId); }
            @Override public void remove(ResourceLocation nodeId) { applied.remove(nodeId); }
        });
        assertEquals(Set.of(id("b")), applied, "a re-removed, b re-added");
    }

    @Test
    void spentPointsExemptsRootsAndSealedNodes() {   // 技能点公式: applied - roots - sealed
        var applied = Set.of(id("root"), id("a"), id("b"), id("c"));
        var sealed = Set.of(id("c"));
        int spent = BoonLedger.spentPoints(id -> id.getPath().endsWith("root"), applied, sealed);
        assertEquals(2, spent, "Roots never cost points and sealed nodes stop counting");
    }
}
