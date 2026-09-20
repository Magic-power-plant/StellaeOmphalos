package com.mpp.stellaeomphalos.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.player.progress.*;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.*;

class StarRecordTest {
    private static ResourceLocation id(String path) {
        return new ResourceLocation("stellaeomphalos", path);
    }

    @Test
    void completeArchiveSurvivesTenRoundTrips() {
        var record = new StarRecord();
        record.promote(StarTier.RADIANCE);
        record.branch("ATTUNEMENT");
        record.discover(id("aevitas"));
        record.see(id("armara"));
        record.node(id("research/example"));
        record.shard(id("lore/example"));
        record.target(id("target/example"));
        record.read(id("codex/page/example"), "stellaeomphalos:research/example#0");
        record.reward();
        var expected = record.save();
        for (int i = 0; i < 10; i++) {
            record = StarRecord.load(record.save());
            assertEquals(expected, record.save());
        }
        assertTrue(record.seenSigns().containsAll(record.knownSigns()));
        assertTrue(record.branches().contains("DISCOVERY"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> StarRecord.load(expected).knownSigns().clear());
    }

    @Test
    void oldRecordsDoNotReissueTheBookButNewPlayersDo() {
        assertFalse(new StarRecord().firstJoinRewarded());
        assertTrue(StarRecord.load(new CompoundTag()).firstJoinRewarded());
        var newRecord = new StarRecord();
        assertTrue(newRecord.reward());
        assertFalse(newRecord.reward());
    }

    @Test
    void fakeRecordRejectsEveryMutation() {
        var record = new FakeStarRecord();
        var before = record.save();
        var input = new StarRecord();
        input.promote(StarTier.BRILLIANCE);
        assertFalse(record.promote(StarTier.BRILLIANCE));
        assertFalse(record.branch("RADIANCE"));
        assertFalse(record.node(id("x")));
        assertFalse(record.shard(id("x")));
        assertFalse(record.target(id("x")));
        assertFalse(record.discover(id("x")));
        assertFalse(record.see(id("x")));
        assertFalse(record.reward());
        assertFalse(record.read(id("x"), "x"));
        assertFalse(record.captureBoons(new CompoundTag()));
        assertFalse(record.merge(input.share()));
        assertEquals(before, record.save());
        assertFalse(record.valid());
        assertTrue(record.branches().isEmpty());
    }

    @Test
    void sharingUsesAWhitelistAndNeverImportsEconomyOrShards() {
        var sender = new StarRecord();
        sender.promote(StarTier.BRILLIANCE);
        sender.discover(id("aevitas"));
        sender.node(id("x"));
        sender.shard(id("private"));
        var receiver = new StarRecord();
        receiver.see(id("armara"));
        var economy = new CompoundTag();
        economy.putLong("BoonExp", 700);
        economy.putInt("BoonTokens", 12);
        receiver.captureBoons(economy);
        var payload = sender.save(); // Even a malicious full archive is safely cropped by merge.
        var injected = new CompoundTag();
        injected.putLong("BoonExp", Long.MAX_VALUE);
        payload.put("Boons", injected);
        assertTrue(receiver.merge(payload));
        assertFalse(receiver.merge(payload));
        assertEquals(economy, receiver.boonSnapshot());
        assertEquals(StarTier.BRILLIANCE, receiver.tier());
        assertTrue(receiver.seenSigns().containsAll(Set.of(id("aevitas"), id("armara"))));
        assertTrue(receiver.unlockedShards().isEmpty());
        assertFalse(receiver.firstJoinRewarded());
    }

    @Test
    void futureRecordsArePreservedAndCannotBeMutated() {
        var future = new CompoundTag();
        future.putInt("RecordVersion", StarRecord.VERSION + 1);
        future.putString("FutureField", "precious");
        assertThrows(StarRecord.FutureRecordException.class, () -> StarRecord.load(future));
        var players = new CompoundTag();
        var uuid = UUID.randomUUID();
        players.put(uuid.toString(), future);
        var input = new CompoundTag();
        input.put("Players", players);
        var store = new StarRecordStore(input, new StarRecordIO(StarRecordIO.DISK), message -> {});
        assertFalse(store.record(uuid).reward());
        assertEquals(
                future,
                store.save(new CompoundTag()).getCompound("Players").getCompound(uuid.toString()));
    }

    @Test
    void malformedPlayerDoesNotEraseOtherPlayersAndEvidenceDoesNotMultiply() {
        var players = new CompoundTag();
        var valid = UUID.randomUUID();
        var invalid = UUID.randomUUID();
        var record = new StarRecord();
        record.discover(id("aevitas"));
        players.put(valid.toString(), record.save());
        players.putString(invalid.toString(), "bad");
        var input = new CompoundTag();
        input.put("Players", players);
        var store = new StarRecordStore(input, new StarRecordIO(StarRecordIO.DISK), m -> {});
        assertEquals(record.knownSigns(), store.record(valid).knownSigns());
        assertTrue(store.record(invalid).branches().contains("DISCOVERY"));
        for (int i = 0; i < 10; i++)
            store =
                    new StarRecordStore(
                            store.save(new CompoundTag()),
                            new StarRecordIO(StarRecordIO.DISK),
                            m -> {});
        assertEquals(3, store.save(new CompoundTag()).getCompound("Players").size());
    }

    @Test
    void invalidRouteAndCountsAreBounded() {
        var record = new StarRecord();
        assertFalse(record.read(id("page"), "x".repeat(513)));
        var tag = record.save();
        tag.putString("CodexLastRoute", "x".repeat(514));
        var read = new CompoundTag();
        read.putInt("stellaeomphalos:page", -100);
        tag.put("CodexSeen", read);
        var loaded = StarRecord.load(tag);
        assertEquals("", loaded.lastRoute());
        assertEquals(0, loaded.codexSeen().get(id("page")));
    }

    @Test
    void publicPoolClaimsAndSeedSurviveReloadWithoutAffectingIndependentRecords() {
        var store =
                new StarRecordStore(
                        new CompoundTag(), new StarRecordIO(StarRecordIO.DISK), m -> {});
        long seed = store.poolSeed();
        store.claim(id("lore/a"));
        long counter = store.nextSeed();
        var restored =
                new StarRecordStore(
                        store.save(new CompoundTag()),
                        new StarRecordIO(StarRecordIO.DISK),
                        m -> {});
        assertEquals(seed, restored.poolSeed());
        assertTrue(restored.claimed(id("lore/a")));
        assertEquals(counter + 1, restored.nextSeed());
        restored.renewPool(Set.of(id("lore/a")));
        assertFalse(restored.claimed(id("lore/a")));
    }
}
