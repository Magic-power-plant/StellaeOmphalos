package com.mpp.stellaeomphalos.constellation.effect;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MeteorStrikeEffectTest {
    @Test void centerHitAtFullDayAndNoise() {
        // d = 0: full base 10 + 0.5*40 + 0.5*60 = 60
        assertEquals(60.0F, MeteorStrikeEffect.damageAt(0.0, 10.0, 0.5, 0.5), 1.0E-4);
    }

    @Test void falloffIsLinearWithClampedDistance() {
        float half = MeteorStrikeEffect.damageAt(5.0, 10.0, 0.0, 0.0);
        assertEquals(5.0F, half, 1.0E-4);          // 10 * (1 - 0.5)
        float edge = MeteorStrikeEffect.damageAt(10.0, 10.0, 1.0, 1.0);
        assertEquals(0.0F, edge, 1.0E-4);          // falloff 0 -> below cutoff
        float beyond = MeteorStrikeEffect.damageAt(25.0, 10.0, 1.0, 1.0);
        assertEquals(0.0F, beyond, 1.0E-4);        // clamped to 0 falloff
    }

    @Test void belowHalfDamageIsNotResolved() {
        // base 10 (day 0, noise 0) at falloff 0.04 -> 0.4 < 0.5 -> skipped
        assertEquals(0.0F, MeteorStrikeEffect.damageAt(9.6, 10.0, 0.0, 0.0), 1.0E-4);
        // falloff 0.05 -> exactly 0.5 resolves
        assertEquals(0.5F, MeteorStrikeEffect.damageAt(9.5, 10.0, 0.0, 0.0), 1.0E-4);
    }

    @Test void dayAndNoiseTermsScaleDamage() {
        float calmNight = MeteorStrikeEffect.damageAt(0.0, 10.0, 0.0, 0.0);
        float noon = MeteorStrikeEffect.damageAt(0.0, 10.0, 1.0, 0.0);
        float noisy = MeteorStrikeEffect.damageAt(0.0, 10.0, 0.0, 1.0);
        assertEquals(10.0F, calmNight, 1.0E-4);
        assertEquals(50.0F, noon, 1.0E-4);
        assertEquals(70.0F, noisy, 1.0E-4);
    }

    @Test void nonpositiveRadiusDealsNothing() {
        assertEquals(0.0F, MeteorStrikeEffect.damageAt(0.0, 0.0, 1.0, 1.0));
    }
}
