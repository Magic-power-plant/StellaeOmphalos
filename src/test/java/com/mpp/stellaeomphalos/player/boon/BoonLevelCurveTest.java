package com.mpp.stellaeomphalos.player.boon;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonLevelCurveTest {

    @Test
    void curveMatchesTheContractFormula() {
        // expForLevel(i) = prev + 150 + floor(2^((i/2)+3)), integer division on i.
        assertEquals(0, BoonLevelCurve.expForLevel(1));
        assertEquals(166, BoonLevelCurve.expForLevel(2));                    // 150 + 2^(1+3)
        assertEquals(332, BoonLevelCurve.expForLevel(3));                    // +150 + 2^(1+3)
        assertEquals(514, BoonLevelCurve.expForLevel(4));                    // +150 + 2^(2+3)
        assertEquals(696, BoonLevelCurve.expForLevel(5));                    // +150 + 2^(2+3)
        for (int i = 2; i <= 30; i++)
            assertTrue(BoonLevelCurve.expForLevel(i) > BoonLevelCurve.expForLevel(i - 1), "Curve must increase at " + i);
        assertThrows(IllegalArgumentException.class, () -> BoonLevelCurve.expForLevel(0));
    }

    @Test
    void levelForExpBoundariesAndClamp() {
        assertEquals(1, BoonLevelCurve.levelForExp(0, 30));
        assertEquals(1, BoonLevelCurve.levelForExp(165, 30), "One below the band stays at level 1");
        assertEquals(2, BoonLevelCurve.levelForExp(166, 30), "Exact threshold promotes");
        assertEquals(2, BoonLevelCurve.levelForExp(331, 30));
        assertEquals(3, BoonLevelCurve.levelForExp(332, 30));
        assertEquals(5, BoonLevelCurve.levelForExp(Long.MAX_VALUE / 2, 5), "Level clamps at the cap");
        assertEquals(30, BoonLevelCurve.levelForExp(BoonLevelCurve.expForLevel(30), 30));
        assertThrows(IllegalArgumentException.class, () -> BoonLevelCurve.levelForExp(0, 0));
    }

    @Test
    void spanOfMatchesCurveDifference() {
        assertEquals(166, BoonLevelCurve.spanOf(1));
        assertEquals(182, BoonLevelCurve.spanOf(3));
    }
}
