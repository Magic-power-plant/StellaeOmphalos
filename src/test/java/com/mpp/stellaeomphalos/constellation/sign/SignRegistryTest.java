package com.mpp.stellaeomphalos.constellation.sign;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class SignRegistryTest {
    @Test void registrationOrderDefinesNumericIds() {
        var first = SignTestSupport.major("reg_a");
        var second = SignTestSupport.ritual("reg_b");
        var third = SignTestSupport.trait("reg_c", MoonPhase.FULL);
        SignRegistry.rebuild(List.of(first, second, third));
        assertEquals(0, SignRegistry.numericId(first));
        assertEquals(1, SignRegistry.numericId(second));
        assertEquals(2, SignRegistry.numericId(third));
        assertSame(second, SignRegistry.byNumericId(1));
        assertSame(third, SignRegistry.byId(SignTestSupport.id("reg_c")));
        assertNull(SignRegistry.byNumericId(99));
        assertNull(SignRegistry.byId(SignTestSupport.id("missing")));
        assertEquals(3, SignRegistry.all().size());
    }

    @Test void duplicateIdsFailFast() {
        var sign = SignTestSupport.ritual("reg_dup");
        assertThrows(IllegalStateException.class, () -> SignRegistry.rebuild(List.of(sign, sign)));
        assertThrows(IllegalStateException.class,
                () -> SignRegistry.rebuild(List.of(sign, SignTestSupport.ritual("reg_dup"))));
        SignRegistry.rebuild(List.of(sign));
        assertSame(sign, SignRegistry.byId(SignTestSupport.id("reg_dup")));
    }

    @Test void categoryListsSplitByInterface() {
        var major = SignTestSupport.major("reg_major");
        var ritual = SignTestSupport.ritual("reg_ritual");
        var trait = SignTestSupport.trait("reg_trait", MoonPhase.NEW);
        SignRegistry.rebuild(List.of(major, ritual, trait));
        assertEquals(List.of(major), SignRegistry.majorSigns());
        assertEquals(List.of(ritual), SignRegistry.ritualSigns());
        assertEquals(List.of(trait), SignRegistry.traitSigns());
        assertTrue(SignRegistry.anomalousSigns().isEmpty());
    }

    @Test void freezeBlocksIndividualRegistrationButNotReloadRebuild() {
        SignRegistry.freeze();
        assertTrue(SignRegistry.frozen());
        assertThrows(IllegalStateException.class, () -> SignRegistry.register(SignTestSupport.ritual("reg_late")));
        var replacement = SignTestSupport.ritual("reg_reload");
        SignRegistry.rebuild(List.of(replacement));
        assertSame(replacement, SignRegistry.byNumericId(0));
    }
}
