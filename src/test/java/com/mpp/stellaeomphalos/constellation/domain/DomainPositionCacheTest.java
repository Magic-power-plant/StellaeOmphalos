package com.mpp.stellaeomphalos.constellation.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.CounterCapEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.CounterEntry;
import com.mpp.stellaeomphalos.constellation.domain.DomainPositionEntries.TaggedTupleEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class DomainPositionCacheTest {
    private static final class TestCache extends DomainPositionCache<CounterEntry> {
        TestCache(int cap) { super(null, cap, pos -> true, CounterEntry::new); }
        @Override public boolean play(DomainContext ctx, float strength, DomainProperties props) { return false; }
        @Override public DomainProperties provideProperties(int mirrorCount) { return new DomainProperties(6, 1, 1, false, 0, 1); }
    }

    @Test void offerDedupesAndEnforcesCap() {
        var cache = new TestCache(2);
        var pos = new BlockPos(1, 2, 3);
        assertTrue(cache.offer(new CounterEntry(pos)));
        assertFalse(cache.offer(new CounterEntry(pos)), "duplicate position must be rejected");
        assertTrue(cache.offer(new CounterEntry(new BlockPos(4, 5, 6))));
        assertFalse(cache.offer(new CounterEntry(new BlockPos(7, 8, 9))), "cap must be enforced");
        assertEquals(2, cache.size());
    }

    @Test void nbtRoundtripPreservesEntries() {
        var cache = new TestCache(10);
        var a = new CounterEntry(new BlockPos(10, 64, -20));
        a.add(7);
        cache.offer(a);
        cache.offer(new CounterEntry(new BlockPos(-5, 70, 6)));
        var tag = new CompoundTag();
        cache.write(tag);

        var restored = new TestCache(10);
        restored.read(tag);
        assertEquals(2, restored.size());
        assertEquals(new BlockPos(10, 64, -20), restored.entries().get(0).pos());
        assertEquals(7, restored.entries().get(0).counter());
        assertEquals(new BlockPos(-5, 70, 6), restored.entries().get(1).pos());
    }

    @Test void warmupCurveBoundaries() {
        var random = RandomSource.create(42L);
        assertFalse(DomainPositionCache.warmupHit(12, 0, random), "empty cache never hits");
        for (int i = 0; i < 100; i++) {
            assertTrue(DomainPositionCache.warmupHit(12, 12, random), "full cache must always hit");
            assertTrue(DomainPositionCache.warmupHit(12, 9, random), "(cap-size)/4 == 0 must always hit");
        }
        // size == cap - 4: nextInt(2) == 0 has probability 1/2; over 4000 draws both outcomes must appear
        var seeded = RandomSource.create(7L);
        boolean hit = false;
        boolean miss = false;
        for (int i = 0; i < 4000 && !(hit && miss); i++) {
            if (DomainPositionCache.warmupHit(12, 8, seeded)) hit = true;
            else miss = true;
        }
        assertTrue(hit && miss, "warmup curve must be probabilistic below the full mark");
    }

    @Test void randomByChanceReturnsOnlyCachedEntries() {
        var cache = new TestCache(4);
        cache.offer(new CounterEntry(new BlockPos(0, 0, 0)));
        cache.offer(new CounterEntry(new BlockPos(1, 0, 0)));
        var random = RandomSource.create(99L);
        for (int i = 0; i < 100; i++) {
            var entry = cache.randomByChance(random);
            if (entry != null) assertTrue(cache.entries().contains(entry));
        }
    }

    @Test void counterCapEntryTickRespectsCap() {
        var entry = new CounterCapEntry(new BlockPos(0, 0, 0), 3);
        assertFalse(entry.tick());
        assertFalse(entry.tick());
        assertTrue(entry.tick());
        assertEquals(3, entry.counter());
        assertTrue(entry.tick(), "ticking at the cap stays due");
    }

    @Test void counterCapEntryClampsCapFloor() {
        var entry = new CounterCapEntry(new BlockPos(0, 0, 0));
        entry.setCap(-5);   // plan force-fix: cap never below the floor
        assertEquals(1, entry.cap());
    }

    @Test void entryNbtKeys() {
        var tuple = new TaggedTupleEntry(new BlockPos(1, 1, 1));
        tuple.set(new CompoundTag(), new CompoundTag());
        var tag = new CompoundTag();
        tuple.write(tag);
        assertTrue(tag.contains("Pos") && tag.contains("Key") && tag.contains("Value"));
        var counter = new CounterEntry(new BlockPos(2, 2, 2));
        counter.add(3);
        var counterTag = new CompoundTag();
        counter.write(counterTag);
        assertTrue(counterTag.contains("Pos") && counterTag.contains("Counter"));
        assertEquals(3, counterTag.getInt("Counter"));
    }

    @Test void clearCacheEmptiesFocusedList() {
        var cache = new TestCache(4);
        cache.offer(new CounterEntry(new BlockPos(0, 0, 0)));
        cache.clearCache();
        assertEquals(0, cache.size());
    }
}
