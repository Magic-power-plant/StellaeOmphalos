package com.mpp.stellaeomphalos.constellation.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.constellation.sign.AbstractSign;
import com.mpp.stellaeomphalos.constellation.sign.TraitSign;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DomainPropertiesTest {
    private static TraitSign trait(String path) {
        return new AbstractSign.Trait(new ResourceLocation("stellaeomphalos", path), 0, List.of(), Set.of());
    }

    private static DomainProperties base() {
        return new DomainProperties(10.0, 2.0, 1.5, false, 4.0, 6.0);
    }

    @Test void nullTraitIsIdentity() {
        assertEquals(base(), base().modify(null));
    }

    @Test void geluWideLowPotency() {
        var modified = base().modify(trait("gelu"));
        assertEquals(2.0 * 0.15, modified.potency(), 1.0E-9);
        assertEquals(10.0 * 3.5, modified.size(), 1.0E-9);
        assertEquals(1.5, modified.effectAmplifier(), 1.0E-9);
        assertFalse(modified.corrupted());
    }

    @Test void ulteriaSmallHighAmplifier() {
        var modified = base().modify(trait("ulteria"));
        assertEquals(10.0 * 0.2, modified.size(), 1.0E-9);
        assertEquals(1.5 * 4.0, modified.effectAmplifier(), 1.0E-9);
        assertEquals(2.0, modified.potency(), 1.0E-9);
    }

    @Test void alcaraCorruptsAndScalesFracture() {
        var modified = base().modify(trait("alcara"));
        assertTrue(modified.corrupted());
        assertEquals(10.0 * 2.0, modified.size(), 1.0E-9);
        assertEquals(1.5 * 2.0, modified.effectAmplifier(), 1.0E-9);
        assertEquals(4.0 * 0.015, modified.fractureLower(), 1.0E-9);
        assertEquals(6.0 * 50000.0, modified.fractureRate(), 1.0E-6);
    }

    @Test void voruxScalesFractureWithoutCorruption() {
        var modified = base().modify(trait("vorux"));
        assertFalse(modified.corrupted());
        assertEquals(10.0 * 1.75, modified.size(), 1.0E-9);
        assertEquals(1.5 * 2.0, modified.effectAmplifier(), 1.0E-9);
        assertEquals(4.0 * 0.25, modified.fractureLower(), 1.0E-9);
        assertEquals(6.0 * 3000.0, modified.fractureRate(), 1.0E-6);
    }

    @Test void unknownTraitIsNeutral() {
        assertEquals(base(), base().modify(trait("nonexistent")));
    }

    /** The documented accumulation hazard: a second modify squares the scales. */
    @Test void repeatedModifyAccumulatesMultiplicatively() {
        var once = base().modify(trait("ulteria"));
        var twice = once.modify(trait("ulteria"));
        assertEquals(10.0 * 0.2 * 0.2, twice.size(), 1.0E-9);
        assertEquals(1.5 * 4.0 * 4.0, twice.effectAmplifier(), 1.0E-9);
        assertNotEquals(once, twice);
    }
}
