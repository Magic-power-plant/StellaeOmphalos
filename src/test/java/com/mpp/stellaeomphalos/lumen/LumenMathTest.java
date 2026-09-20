package com.mpp.stellaeomphalos.lumen;

import com.mpp.stellaeomphalos.lumen.transport.EpochCache;
import com.mpp.stellaeomphalos.lumen.transport.LumenMath;
import com.mpp.stellaeomphalos.lumen.transport.LumenWorkQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LumenMathTest {
    @Test void sectionIndexUsesFloorDivAndToleratesOutOfRangeHeights() {
        assertEquals(0, LumenMath.sectionY(-64, -64));
        assertEquals(4, LumenMath.sectionY(0, -64));
        assertEquals(24, LumenMath.sectionY(320, -64));
        assertEquals(-1, LumenMath.sectionY(-65, -64));
        assertEquals(3, LumenMath.sectionY(-1, -64));
        assertEquals(0, LumenMath.sectionY(0, 0));
        assertEquals(-1, LumenMath.sectionY(-1, 0));
    }

    @Test void hopLossAddsHeightPenaltyBeyondEightBlocksAndClamps() {
        assertEquals(0.02, LumenMath.hopLoss(0.02, 0), 1e-9);
        assertEquals(0.02, LumenMath.hopLoss(0.02, 8), 1e-9);
        assertEquals(0.03, LumenMath.hopLoss(0.02, 9), 1e-9);
        assertEquals(0.14, LumenMath.hopLoss(0.02, 20), 1e-9);
        assertEquals(0.95, LumenMath.hopLoss(0.5, 200), 1e-9);
    }

    @Test void deliveryAppliesPerHopCompounding() {
        assertEquals(1000, LumenMath.delivered(1000, 0.02, 0));
        assertEquals(980, LumenMath.delivered(1000, 0.02, 1));
        assertEquals(960, LumenMath.delivered(1000, 0.02, 2));
        assertEquals(524, LumenMath.delivered(1000, 0.02, 32));
        assertEquals(0, LumenMath.delivered(-5, 0.02, 3));
        assertEquals(1000, LumenMath.scale(1000, 2.0));
    }

    @Test void sourceOutputFollowsTheFrozenFormula() {
        assertEquals(0, LumenMath.sourceOutput(1000, false, 1.0, false, 0, 1));
        assertEquals(0, LumenMath.sourceOutput(1000, true, 0, false, 0, 1));
        assertEquals(1000, LumenMath.sourceOutput(1000, true, 1.0, false, 0, 1));
        assertEquals(600, LumenMath.sourceOutput(1000, true, 0.5, false, 0, 1));
        assertEquals(1700, LumenMath.sourceOutput(1000, true, 1.0, true, 0, 1));
        assertEquals(850, LumenMath.sourceOutput(1000, true, 1.0, true, 0, 0.5));
        assertEquals(1300, LumenMath.sourceOutput(1000, true, 1.0, false, 1.0, 1));
        assertEquals(500, LumenMath.sourceOutput(1000, true, 1.0, false, 0, 0.5));
        assertEquals(650, LumenMath.sourceOutput(1000, true, 1.0, false, 1.0, 0.5));
    }

    @Test void proximityPenaltyKicksInBelowSixteenBlocks() {
        assertEquals(1, LumenMath.proximityPenalty(16), 1e-9);
        assertEquals(1, LumenMath.proximityPenalty(100), 1e-9);
        assertEquals(0.5, LumenMath.proximityPenalty(8), 1e-9);
        assertEquals(0.0625, LumenMath.proximityPenalty(1), 1e-9);
    }

    @Test void epochCacheRecomputesOnlyOnEpochChange() {
        var cache = new EpochCache<String>();
        var calls = new AtomicInteger();
        assertEquals("v1", cache.get(1, () -> "v" + calls.incrementAndGet()));
        assertEquals("v1", cache.get(1, () -> "v" + calls.incrementAndGet()));
        assertEquals(1, calls.get());
        assertEquals("v2", cache.get(2, () -> "v" + calls.incrementAndGet()));
        cache.invalidate();
        assertEquals("v3", cache.get(2, () -> "v" + calls.incrementAndGet()));
        assertEquals(3, calls.get());
        assertNull(cache.get(9, () -> null));
        assertEquals("v4", cache.get(9, () -> "v" + calls.incrementAndGet()));
    }

    @Test void workQueueDeduplicatesAndHonoursPerTickBudget() {
        var queue = new LumenWorkQueue();
        for (long i = 0; i < 100; i++) queue.offer(i);
        queue.offer(7);
        assertEquals(100, queue.size());
        var drained = new ArrayList<Long>();
        int total = 0;
        int rounds = 0;
        while (!queue.isEmpty()) {
            total += queue.process(32, drained::add);
            rounds++;
            assertTrue(rounds <= 4, "Throttle must drain 100 items in 4 ticks of 32");
        }
        assertEquals(4, rounds);
        assertEquals(100, total);
        assertEquals(100, drained.stream().distinct().count());
        assertEquals(0, queue.process(32, key -> fail("Empty queue must not process")));
    }

    @Test void workQueueToleratesReofferDuringProcessing() {
        var queue = new LumenWorkQueue();
        queue.offer(1);
        var seen = new ArrayList<Long>();
        assertEquals(1, queue.process(32, key -> {
            seen.add(key);
            if (key == 1) queue.offer(2);
        }));
        assertEquals(List.of(1L), seen);
        assertEquals(1, queue.size());
        assertEquals(1, queue.process(32, seen::add));
        assertEquals(List.of(1L, 2L), seen);
        assertTrue(queue.isEmpty());
    }
}
