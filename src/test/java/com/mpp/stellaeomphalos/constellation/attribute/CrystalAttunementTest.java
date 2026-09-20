package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrystalAttunementTest {

    @Test void randomGenerationStaysWithinTriangularBounds() {
        var random = RandomSource.create(42);
        for (int i = 0; i < 200; i++) {
            var attunement = CrystalAttunement.random(random);
            assertTrue(attunement.size() >= 0 && attunement.size() <= 400, "size " + attunement.size());
            assertTrue(attunement.purity() >= 0 && attunement.purity() <= 100, "purity " + attunement.purity());
            assertTrue(attunement.collect() >= 0 && attunement.collect() <= 100, "collect " + attunement.collect());
            assertEquals(0, attunement.fracture());
        }
    }

    @Test void triangularAveragesTwoRolls() {
        var random = RandomSource.create(7);
        int value = CrystalAttunement.triangular(random, 10);
        assertTrue(value >= 0 && value <= 10);
    }

    @Test void grindDestroysCrystalAtZeroSize() {
        var crystal = new CrystalAttunement(5, 100, 50, 0);
        assertNull(crystal.grind(RandomSource.create(1), 0));
        assertNull(new CrystalAttunement(0, 100, 50, 0).grind(RandomSource.create(1), 0));
    }

    @Test void grindSuccessAppliesBaseLossAndBoostsQuality() {
        var random = RandomSource.create(3);
        var crystal = new CrystalAttunement(300, 100, 50, 0);
        var ground = crystal.grind(random, 0);
        assertNotNull(ground);
        int loss = 300 - ground.size();
        assertTrue(loss >= 6 && loss <= 12, "base loss " + loss);
        assertTrue(ground.collect() >= 50);
        assertEquals(0, ground.fracture());
        // 纯度已达上限时不再上涨
        assertEquals(100, ground.purity());
    }

    @Test void grindFailureDoublesLossPerConsecutiveFailure() {
        var crystal = new CrystalAttunement(400, 0, 50, 0);
        var first = crystal.grind(RandomSource.create(9), 0);
        assertNotNull(first);
        int firstLoss = 400 - first.size();
        assertEquals(1, first.fracture());
        // 连续失败 4 次后：损耗为基准的 16 倍
        var fifth = first.grind(RandomSource.create(9), 4);
        assertNotNull(fifth);
        int streakLoss = first.size() - fifth.size();
        assertTrue(streakLoss >= 6 * 16 && streakLoss <= 12 * 16, "streak loss " + streakLoss);
        assertTrue(streakLoss > firstLoss);
        assertEquals(2, fifth.fracture());
    }

    @Test void fuseSumsSizeAndAveragesQuality() {
        var a = new CrystalAttunement(100, 80, 40, 1);
        var b = new CrystalAttunement(50, 60, 20, 3);
        var fused = ToolCrystalAttunement.fuse(List.of(a, b));
        assertEquals(150, fused.size());
        assertEquals(70.0, fused.purity(), 1e-9);
        assertEquals(30.0, fused.collect(), 1e-9);
        assertEquals(3, fused.fracture());
        assertThrows(IllegalArgumentException.class, () -> ToolCrystalAttunement.fuse(List.of()));
    }

    @Test void efficiencyUsesSquareRootWithFloor() {
        assertEquals(1.0, new ToolCrystalAttunement(100, 0, 100, 0).efficiency(), 1e-9);
        assertEquals(0.5, new ToolCrystalAttunement(100, 0, 25, 0).efficiency(), 1e-9);
        assertEquals(0.05, new ToolCrystalAttunement(100, 0, 0, 0).efficiency(), 1e-9);
    }

    @Test void copyDamagedCuttingReducesSizeOrDestroys() {
        var tool = new ToolCrystalAttunement(50, 80, 100, 0);
        var cut = tool.copyDamagedCutting(20);
        assertNotNull(cut);
        assertEquals(30, cut.size());
        assertEquals(80.0, cut.purity(), 1e-9);
        assertNull(tool.copyDamagedCutting(50));
        assertNull(tool.copyDamagedCutting(51));
    }

    @Test void attunementNbtRoundTrips() {
        var crystal = new CrystalAttunement(120, 90, 45, 2, 400, 100, 100);
        var loaded = CrystalAttunement.load(crystal.save());
        assertEquals(120, loaded.size());
        assertEquals(90, loaded.purity());
        assertEquals(45, loaded.collect());
        assertEquals(2, loaded.fracture());
        assertEquals(400, loaded.maxSize());
    }
}
