package com.mpp.stellaeomphalos.player.boon;

import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonProgressStorageTest {

    private static final Predicate<ResourceLocation> ALL = id -> true;

    private static ResourceLocation id(String path) {
        return new ResourceLocation("stellaeomphalos", path);
    }

    private static BoonProgress.Storage sample() {
        var nodeData = new CompoundTag();
        nodeData.putInt("SocketSeen", 1);
        var data = new CompoundTag();
        data.put("stellaeomphalos:aevitas/root", nodeData);
        return new BoonProgress.Storage(BoonTree.BOON_TREE_VERSION,
                List.of(id("aevitas/root"), id("aevitas/vitality")), List.of(), List.of("token_a", "token_b"), 4321L,
                data, List.of(id("aevitas")), List.of(id("aevitas"), id("vicio")), id("aevitas"));
    }

    @Test
    void storageRoundTripPreservesEveryField() {
        var storage = sample();
        var tag = BoonProgress.saveStorage(new CompoundTag(), storage);
        var restored = BoonProgress.loadStorage(tag, ALL, ALL);
        assertEquals(storage, restored, "Storage must survive the NBT roundtrip");
    }

    @Test
    void unknownNodesAndDanglingSealsDropSilently() {
        var tag = BoonProgress.saveStorage(new CompoundTag(), sample());
        var applied = (ListTag) tag.get("Applied");
        applied.add(StringTag.valueOf("stellaeomphalos:removed/node"));
        tag.getList("Sealed", net.minecraft.nbt.Tag.TAG_STRING)
                .add(StringTag.valueOf("stellaeomphalos:sealed/without_apply"));
        var restored = BoonProgress.loadStorage(tag, id -> !id.getPath().startsWith("removed/"), ALL);
        assertFalse(restored.applied().contains(id("removed/node")), "Unknown node ids are silently dropped");
        assertTrue(restored.sealed().isEmpty(), "Sealed entries without an applied node drop");
        assertEquals(sample().knownSigns(), restored.knownSigns());
    }

    @Test
    void freeTokensDeduplicateAndKnownSignsFoldIntoSeen() {
        var tag = BoonProgress.saveStorage(new CompoundTag(), sample());
        var tokens = new ListTag();
        tokens.add(StringTag.valueOf("dup"));
        tokens.add(StringTag.valueOf("dup"));
        tokens.add(StringTag.valueOf("other"));
        tag.put("FreeTokens", tokens);
        var restored = BoonProgress.loadStorage(tag, ALL, ALL);
        assertEquals(List.of("dup", "other"), restored.freeTokens(), "Tokens deduplicate on load");
        // sample() already folds aevitas into seen; verify the fold adds nothing twice.
        assertEquals(Set.copyOf(restored.knownSigns()).size(), restored.knownSigns().size());
        assertTrue(restored.seenSigns().containsAll(restored.knownSigns()), "Known signs must appear in seen signs");
    }

    @Test
    void staleTreeVersionClearsTreeAndRebuildsAttunedRoot() {   // AC-2.16 pure part
        var stale = new BoonProgress.Storage(0,
                List.of(id("aevitas/root"), id("aevitas/vitality")), List.of(id("aevitas/vitality")),
                List.of("token_a"), 999L, new CompoundTag(),
                List.of(id("aevitas")), List.of(id("aevitas")), id("aevitas"));
        var migrated = BoonProgress.migrate(stale, sign -> id(sign.getPath() + "/root"), ALL);
        assertEquals(BoonTree.BOON_TREE_VERSION, migrated.treeVersion());
        assertEquals(List.of(id("aevitas/root")), migrated.applied(), "Only the attuned sign's root is rebuilt");
        assertTrue(migrated.sealed().isEmpty() && migrated.freeTokens().isEmpty(), "Seals and tokens clear on migration");
        assertEquals(999L, migrated.exp(), "Experience survives a tree migration");

        var current = BoonProgress.migrate(sample(), sign -> null, ALL);
        assertEquals(sample(), current, "Up-to-date storage passes migration untouched");

        var staleNoRoot = BoonProgress.migrate(stale, sign -> id("missing/root"),
                node -> !node.getPath().startsWith("missing/"));
        assertTrue(staleNoRoot.applied().isEmpty(), "A vanished attuned root rebuilds to an empty tree");
    }

    @Test
    void expThrottleDiscardsAtCapAndClampsToEightPercent() {
        int max = 5;
        long capExp = BoonLevelCurve.expForLevel(max);
        assertEquals(capExp, BoonProgress.cappedGain(capExp, 1000, max), "Full-level positive gains are discarded");
        // Level 1 band spans 166; one call caps at floor(166 * 0.08) = 13.
        assertEquals(13, BoonProgress.cappedGain(0, 1000, max));
        assertEquals(5, BoonProgress.cappedGain(0, 5, max), "Small grants pass through");
        // Level 2 band spans 166 as well; at band start the cap is identical.
        assertEquals(166 + 13, BoonProgress.cappedGain(166, 1000, max));
        // Near the top the current band (level 4, span 182) caps at floor(182 * 0.08) = 14.
        long nearTop = BoonLevelCurve.expForLevel(max) - 100;
        assertEquals(nearTop + 14, BoonProgress.cappedGain(nearTop, 1000, max));
        assertEquals(capExp, BoonProgress.cappedGain(capExp - 1, 1000, max), "Gains clamp at the level cap's total");
    }

    @Test
    void deathPenaltyRemovesAQuarterOfTheCurrentBand() {
        int max = 30;
        long floor = BoonLevelCurve.expForLevel(2);
        assertEquals(floor + 75, BoonProgress.deathPenaltyExp(floor + 100, max), "100 in-band -> keep 75");
        assertEquals(floor, BoonProgress.deathPenaltyExp(floor, max), "Band floor is safe");
        assertEquals(0, BoonProgress.deathPenaltyExp(0, max));
        long capExp = BoonLevelCurve.expForLevel(max);
        assertEquals(capExp, BoonProgress.deathPenaltyExp(capExp, max), "Capped experience is not penalized below its floor");
    }
}
