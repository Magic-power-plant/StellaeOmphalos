package com.mpp.stellaeomphalos.player.boon.root;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RootBoonMathTest {

    @Test
    void verdanceDiversityDecaysToFortyPercentFloor() {
        assertEquals(1.0, VerdanceRootBoon.diversityFactor(List.of()), 1.0E-9, "No history pays full");
        var monotone = Collections.nCopies(VerdanceRootBoon.HISTORY, "minecraft:stone");
        assertEquals(0.4, VerdanceRootBoon.diversityFactor(monotone), 1.0E-9, "Monotone placement hits the 0.4 floor");
        var diverse = new ArrayList<String>();
        for (int i = 0; i < VerdanceRootBoon.HISTORY; i++) diverse.add("minecraft:block" + i);
        assertEquals(1.0, VerdanceRootBoon.diversityFactor(diverse), 1.0E-9, "Fully diverse history pays full");
        var half = new ArrayList<String>();
        for (int i = 0; i < 20; i++) half.add("minecraft:block" + (i % 10));
        assertEquals(0.5, VerdanceRootBoon.diversityFactor(half), 1.0E-9, "Half-diverse history pays 0.5");
        var mostlyMono = new ArrayList<String>(Collections.nCopies(19, "minecraft:stone"));
        mostlyMono.add("minecraft:dirt");
        assertEquals(0.4, VerdanceRootBoon.diversityFactor(mostlyMono), 1.0E-9, "Near-monotone clamps to the floor");
    }

    @Test
    void upheavalSquareRootCompressesHardness() {
        assertEquals(0.0, UpheavalRootBoon.compress(0), 1.0E-9);
        assertEquals(0.0, UpheavalRootBoon.compress(-1), 1.0E-9, "Unbreakable blocks pay nothing");
        assertEquals(Math.sqrt(50 * 0.15), UpheavalRootBoon.compress(50), 1.0E-9, "Obsidian-class hardness compresses");
        assertTrue(UpheavalRootBoon.compress(50) < 50 * 0.15, "Square root must shrink large values");
        assertEquals(Math.sqrt(1.5 * 0.15), UpheavalRootBoon.compress(1.5), 1.0E-9, "Stone-class hardness");
    }

    @Test
    void soarWeightedSumHonorsWeightsAndCap() {
        assertEquals(100, SoarRootBoon.weighted(100, 0, 0, 0));
        assertEquals(120, SoarRootBoon.weighted(0, 100, 0, 0), "Sprint pays 1.2x");
        assertEquals(40, SoarRootBoon.weighted(0, 0, 100, 0), "Flying pays 0.4x");
        assertEquals(80, SoarRootBoon.weighted(0, 0, 0, 100), "Elytra pays 0.8x");
        assertEquals(340, SoarRootBoon.weighted(100, 100, 100, 100), "Kinds add up");
        assertEquals(500, SoarRootBoon.weighted(10000, 0, 0, 0), "Per-tick gain caps at 500");
        assertEquals(0, SoarRootBoon.weighted(-5, -5, -5, -5), "Counter resets never subtract");
    }

    @Test
    void aegisDamageCategoryFactorsMatchSpec() {
        assertEquals(0.01, AegisRootBoon.DamageCategory.LAVA.factor, 1.0E-9);
        assertEquals(0.2, AegisRootBoon.DamageCategory.FIRE.factor, 1.0E-9);
        assertEquals(0.1, AegisRootBoon.DamageCategory.STARVE.factor, 1.0E-9);
        assertEquals(0.05, AegisRootBoon.DamageCategory.DROWN.factor, 1.0E-9);
        assertEquals(0.01, AegisRootBoon.DamageCategory.CACTUS.factor, 1.0E-9);
        assertEquals(1.3, AegisRootBoon.DamageCategory.MOB.factor, 1.0E-9);
        assertEquals(0.0, AegisRootBoon.DamageCategory.OTHER.factor, 1.0E-9);
    }

    @Test
    void severanceFactorMatchesSpec() {
        assertEquals(0.09, SeveranceRootBoon.FACTOR, 1.0E-9);
    }
}
