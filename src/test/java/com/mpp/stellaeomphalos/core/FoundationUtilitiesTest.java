package com.mpp.stellaeomphalos.core;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.util.collections.*;
import com.mpp.stellaeomphalos.core.util.tick.ServerTickScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoundationUtilitiesTest {
    @Test void earlyRegistryReadFailsBeforeCallingSupplier() {
        var ready = new AtomicBoolean(); var called = new AtomicBoolean();
        var reference = new RegistrationGuard<>(new ResourceLocation("stellaeomphalos", "probe"), () -> { called.set(true); return "entry"; }, ready::get);
        assertThrows(IllegalStateException.class, reference::get); assertFalse(called.get());
        ready.set(true); assertEquals("entry", reference.get());
    }
    @Test void schedulerDefersNestedTasksAndHonorsBudgetAndClose() {
        var log = new ArrayList<Integer>(); var scheduler = new ServerTickScheduler(exception -> fail(exception));
        scheduler.schedule(0, () -> { log.add(1); scheduler.schedule(0, () -> log.add(3)); });
        scheduler.schedule(0, () -> log.add(2)); assertTrue(log.isEmpty());
        assertEquals(1, scheduler.advance(1)); assertEquals(List.of(1), log);
        scheduler.advance(10); assertEquals(List.of(1, 2, 3), log);
        scheduler.schedule(100, () -> fail("Closed scheduler ran task")); scheduler.close(); assertEquals(0, scheduler.pendingCount());
        assertThrows(IllegalStateException.class, () -> scheduler.schedule(0, () -> {}));
    }
    @Test void expiryCallbackMayRenewItsOwnKey() {
        var calls = new ArrayList<String>();
        @SuppressWarnings("unchecked") TickExpiryMap<String, String>[] box = new TickExpiryMap[1];
        box[0] = new TickExpiryMap<>((key, value) -> { calls.add(value); if (value.equals("old")) box[0].put(key, "renewed", 2); });
        box[0].put("key", "old", 1); box[0].advance(); assertEquals("renewed", box[0].get("key").orElseThrow());
        box[0].advance(); box[0].advance(); assertEquals(List.of("old", "renewed"), calls); assertEquals(0, box[0].size());
    }
    @Test void uniqueListAndWeightedPoolKeepTheirContracts() {
        var list = new OrderedUniqueList<String>(); assertTrue(list.add("a")); assertFalse(list.add("a")); list.add("b");
        assertEquals(List.of("a", "b"), list.snapshot());
        var pool = new CappedWeightedPool<String>(2); assertTrue(pool.offer(new WeightedEntry<>(0, "zero")));
        assertTrue(pool.offer(new WeightedEntry<>(Integer.MAX_VALUE, "positive"))); assertFalse(pool.offer(new WeightedEntry<>(1, "overflow")));
        for (int i = 0; i < 100; i++) assertEquals("positive", pool.sample(new Random(i)).orElseThrow());
    }
    @Test void spiralIsUniqueAndSearchStopsAtItsBudget() {
        var origin = net.minecraft.core.BlockPos.ZERO;
        var found = new java.util.HashSet<net.minecraft.core.BlockPos>();
        for (var pos : new com.mpp.stellaeomphalos.core.util.world.SpiralBlockWalker(origin, 3)) {
            assertTrue(found.add(pos)); assertTrue(Math.abs(pos.getX()) <= 3 && Math.abs(pos.getZ()) <= 3);
        }
        assertEquals(49, found.size());
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var search = new com.mpp.stellaeomphalos.core.util.world.BlockClusterSearch(origin, 1, 100, pos -> { calls.incrementAndGet(); return true; });
        assertFalse(search.advance(3)); assertEquals(3, calls.get());
        assertTrue(search.advance(100)); assertEquals(27, search.result().size());
        assertThrows(IllegalArgumentException.class, () -> new com.mpp.stellaeomphalos.core.util.world.ConeBlockWalker(origin, 0, 1));
    }
    @Test void skyNoiseIsDeterministicBoundedAndContinuous() {
        var field = new com.mpp.stellaeomphalos.core.util.math.SkyDensityField(42, 32);
        var copy = new com.mpp.stellaeomphalos.core.util.math.SkyDensityField(42, 32);
        for (int x = -100; x < 100; x++) {
            double value = field.sample(x, x * 0.7);
            assertTrue(value >= 0 && value <= 1); assertEquals(value, copy.sample(x, x * 0.7));
            assertEquals(value, field.sample(x + 1.0e-6, x * 0.7), 1.0e-5);
        }
    }
}
