package com.mpp.stellaeomphalos.constellation.starmap;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignImprintEffectRegistryTest {
    @Test void duplicate_registration_throws() {
        var sign = new ResourceLocation("stellaeomphalos", "test_duplicate_sign");
        SignImprintEffectRegistry.register(sign, List.of(), List.of());
        assertThrows(IllegalStateException.class,
                () -> SignImprintEffectRegistry.register(sign, List.of(), List.of()));
        assertTrue(SignImprintEffectRegistry.enchantments(new ResourceLocation("stellaeomphalos", "test_absent_sign")).isEmpty());
    }

    @Test void conflict_abandons_remaining_candidates_of_the_sign() {
        var chosen = SignImprintEffectRegistry.<String, String>selectApplicable(
                List.of("ok1", "conflict", "ok2", "ok3"),
                candidate -> true,
                (candidate, accepted) -> !(candidate.equals("conflict") && accepted.equals("existing")),
                candidate -> candidate,
                List.of("existing"));
        assertEquals(List.of("ok1"), chosen, "first conflict must abandon the rest of this sign");
    }

    @Test void non_applicable_candidates_are_skipped_without_breaking() {
        var chosen = SignImprintEffectRegistry.<String, String>selectApplicable(
                List.of("skip", "take"),
                candidate -> !candidate.equals("skip"),
                (candidate, accepted) -> true,
                candidate -> candidate,
                List.of());
        assertEquals(List.of("take"), chosen);
    }

    @Test void accepted_candidates_join_the_compatibility_pool() {
        var chosen = SignImprintEffectRegistry.<String, String>selectApplicable(
                List.of("a", "a", "b"),
                candidate -> true,
                (candidate, accepted) -> !candidate.equals(accepted),
                candidate -> candidate,
                List.of());
        assertEquals(List.of("a"), chosen, "duplicate candidate conflicts with the accepted one");
    }

    @Test void level_interpolation() {
        assertEquals(1, SignImprintEffectRegistry.levelFor(1, 5, 0.0));
        assertEquals(3, SignImprintEffectRegistry.levelFor(1, 5, 0.5));
        assertEquals(5, SignImprintEffectRegistry.levelFor(1, 5, 1.0));
        assertEquals(3, SignImprintEffectRegistry.levelFor(1, 5, 0.375), "1 + round(1.5) = 3");
    }
}
