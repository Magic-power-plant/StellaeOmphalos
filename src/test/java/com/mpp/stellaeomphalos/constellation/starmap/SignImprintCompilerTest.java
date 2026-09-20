package com.mpp.stellaeomphalos.constellation.starmap;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SignImprintCompilerTest {
    private static final ResourceLocation A = new ResourceLocation("stellaeomphalos", "sign_a");
    private static final ResourceLocation B = new ResourceLocation("stellaeomphalos", "sign_b");
    private static final ResourceLocation C = new ResourceLocation("stellaeomphalos", "sign_c");
    private static final double EPS = 1.0E-9;

    private static SignDrawn stroke(ResourceLocation sign, int x, int z) { return new SignDrawn(sign, x, z); }

    @Test void no_overlap_gives_full_coverage_to_each() {
        var coverages = SignImprintCompiler.coverages(List.of(stroke(A, 0, 0), stroke(B, 100, 100)));
        assertEquals(SignImprintCompiler.shiftDistribution(1.0), coverages.get(A), EPS);
        assertEquals(SignImprintCompiler.shiftDistribution(1.0), coverages.get(B), EPS);
    }

    @Test void full_overlap_zeroes_the_earlier_stroke() {
        // Draw order: A first, then B on the same cell; B is processed first and wins the cell.
        var coverages = SignImprintCompiler.coverages(List.of(stroke(A, 0, 0), stroke(B, 0, 0)));
        assertEquals(SignImprintCompiler.shiftDistribution(1.0), coverages.get(B), EPS);
        assertEquals(SignImprintCompiler.shiftDistribution(0.0), coverages.get(A), EPS);
    }

    @Test void second_order_intersection_is_deduplicated() {
        // Draw order A,B,C; processed reversed: C first (full), then B, then A.
        // B=[10,40]: only C=[15,45] overlaps -> [15,40] = 750 -> coverage 1/6.
        // A=[0,30]: A∩C=[15,30] (450) + A∩B=[10,30] (600) - second-order [15,30] (450) = 600 -> coverage 1/3.
        var coverages = SignImprintCompiler.coverages(List.of(stroke(A, 0, 0), stroke(B, 10, 0), stroke(C, 15, 0)));
        assertEquals(SignImprintCompiler.shiftDistribution(1.0), coverages.get(C), EPS);
        assertEquals(SignImprintCompiler.shiftDistribution(1.0 / 6.0), coverages.get(B), EPS);
        assertEquals(SignImprintCompiler.shiftDistribution(1.0 / 3.0), coverages.get(A), EPS, "second-order dedup lost");
    }

    @Test void multiple_strokes_of_one_sign_are_averaged() {
        var coverages = SignImprintCompiler.coverages(List.of(stroke(A, 0, 0), stroke(A, 100, 100), stroke(A, 0, 0)));
        // Reversed: last-drawn A(0,0) coverage 1, A(100,100) coverage 1, first A(0,0) coverage 0 -> mean 2/3.
        assertEquals(SignImprintCompiler.shiftDistribution(2.0 / 3.0), coverages.get(A), EPS);
    }

    @Test void caller_list_is_never_mutated() {
        var drawn = new ArrayList<>(List.of(stroke(A, 0, 0), stroke(B, 5, 5)));
        SignImprintCompiler.coverages(drawn);
        assertEquals(A, drawn.get(0).sign(), "compile must reverse an internal copy");
        assertEquals(B, drawn.get(1).sign());
    }

    @Test void shift_distribution_endpoints() {
        assertEquals(0.0, SignImprintCompiler.shiftDistribution(0.0), EPS);
        assertEquals(1.0, SignImprintCompiler.shiftDistribution(0.75), EPS);
        assertEquals(Math.sqrt(8.0 / 9.0), SignImprintCompiler.shiftDistribution(0.5), EPS);
        assertEquals(Math.sqrt(1.0 - 0.75 * 0.75), SignImprintCompiler.shiftDistribution(1.0), EPS);
        assertEquals(0.0, SignImprintCompiler.shiftDistribution(1.5), EPS, "out of range clamps to 0");
    }
}
