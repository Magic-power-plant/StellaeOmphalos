package com.mpp.stellaeomphalos.structure;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.content.world.*;
import com.mpp.stellaeomphalos.content.world.capability.*;
import com.mpp.stellaeomphalos.ritual.amplifier.AmplifierTier;
import com.mpp.stellaeomphalos.ritual.effect.RiteEffectDispatcher;
import com.mpp.stellaeomphalos.structure.pattern.PlacementTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import org.junit.jupiter.api.Test;

import java.util.*;

class PartFourMathTest {
    @Test
    void allEightTransformsFormAGroup() {
        for (var a : PlacementTransform.values()) {
            var p = new BlockPos(-9, 3, 7);
            assertEquals(p, a.inverse().apply(a.apply(p)));
            for (var b : PlacementTransform.values())
                assertEquals(a.apply(b.apply(p)), a.compose(b).apply(p));
        }
        assertEquals(
                8,
                Arrays.stream(PlacementTransform.values())
                        .map(t -> t.apply(new BlockPos(3, 0, 7)))
                        .distinct()
                        .count());
    }

    @Test
    void springsAreStableIndependentOfEntryOrderAndSurviveTenReloads() {
        var entries =
                List.of(
                        new SpringFluidEntry(
                                new ResourceLocation("minecraft", "water"),
                                14000,
                                6000,
                                100,
                                "",
                                "COLD"),
                        new SpringFluidEntry(
                                new ResourceLocation("minecraft", "lava"),
                                14000,
                                6000,
                                20,
                                "",
                                "HOT"));
        for (int x = -10; x < 10; x++) {
            var a = new SpringVeinHolder();
            a.initialize(123, x, -5, entries);
            var b = new SpringVeinHolder();
            b.initialize(123, x, -5, List.of(entries.get(1), entries.get(0)));
            assertEquals(a.serializeNBT(), b.serializeNBT());
            int before = a.remainingMb();
            assertEquals(777, a.drain(777, false));
            assertEquals(before, a.remainingMb());
            a.drain(777, true);
            for (int i = 0; i < 10; i++) {
                b = new SpringVeinHolder();
                b.deserializeNBT(a.serializeNBT());
                b.initialize(99, 99, 99, List.of());
                assertEquals(before - 777, b.remainingMb());
                a = b;
            }
        }
    }

    @Test
    void exhaustedAndAbsentSpringsNeverReroll() {
        var a = new SpringVeinHolder();
        a.initialize(2, 3, 4, List.of());
        assertFalse(a.present());
        var n = a.serializeNBT();
        var b = new SpringVeinHolder();
        b.deserializeNBT(n);
        b.initialize(
                6,
                7,
                8,
                List.of(
                        new SpringFluidEntry(
                                new ResourceLocation("minecraft", "water"), 9, 0, 1, "", "ANY")));
        assertFalse(b.present());
    }

    @Test
    void dispatcherRetainsEveryDeferredEffectAndHonorsQuota() {
        var d = new RiteEffectDispatcher();
        var output = new ArrayList<Integer>();
        for (int i = 0; i < 12; i++) {
            int v = i;
            d.submit(i, 4, i % 3, () -> output.add(v));
        }
        for (int i = 0; i < 6; i++) assertEquals(8, d.tick(8));
        assertEquals(12, new HashSet<>(output).size());
        assertEquals(0, d.queued());
    }

    @Test
    void phaseStampsRemainIndependent() {
        var s = new RetroGenStamp();
        s.ready(5);
        s.complete(WorldGenPhase.ORES);
        assertFalse(s.missing(WorldGenPhase.ORES));
        assertTrue(s.missing(WorldGenPhase.SPRING));
        var copy = new RetroGenStamp();
        copy.deserializeNBT(s.serializeNBT());
        copy.ready(5);
        assertEquals(s.stamp(), copy.stamp());
        copy.ready(6);
        assertEquals(0, copy.stamp());
    }

    @Test
    void nearestSearchMatchesBruteForceAcrossGridBoundaries() {
        var ledger = new AstrolabeLedger();
        var id = new ResourceLocation("test", "target");
        var rng = new Random(9);
        var points = new ArrayList<BlockPos>();
        for (int i = 0; i < 4096; i++) {
            var p = new BlockPos(rng.nextInt(4000) - 2000, 64, rng.nextInt(4000) - 2000);
            points.add(p);
            ledger.mark(id, p);
        }
        for (int i = 0; i < 100; i++) {
            var from = new Vec3(rng.nextInt(500) - 250, 70, rng.nextInt(500) - 250);
            var nearest =
                    points.stream()
                            .min(
                                    Comparator.comparingDouble(
                                            p ->
                                                    Math.pow(p.getX() - from.x, 2)
                                                            + Math.pow(p.getZ() - from.z, 2)))
                            .orElseThrow();
            assertEquals(nearest, ledger.nearest(id, from, 256).orElseThrow());
        }
        var copy = AstrolabeLedger.load(ledger.save(new CompoundTag()));
        assertEquals(ledger.size(), copy.size());
        assertTrue(copy.nearest(id, new Vec3(100000, 0, 100000), 1).isEmpty());
    }

    @Test
    void intensityIsClampedAndPenalizesInterrupts() {
        assertEquals(2, AmplifierTier.intensity(1, 100, 0, 0));
        assertTrue(AmplifierTier.intensity(1, 1, 0, 3) < AmplifierTier.intensity(1, 1, 0, 0));
    }
}
