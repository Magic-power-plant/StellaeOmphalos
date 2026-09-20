package com.mpp.stellaeomphalos.constellation.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DomainEntityScanTest {
    @Test void capTruncatesBeyondMaxTargets() {
        var input = IntStream.range(0, 50).boxed().toList();
        var capped = DomainEntityScan.cap(input, 32);
        assertEquals(32, capped.size());
        assertEquals(input.subList(0, 32), capped);
    }

    @Test void capKeepsShortLists() {
        var input = List.of(1, 2, 3);
        assertSame(input, DomainEntityScan.cap(input, 16));
    }

    @Test void capBoundaryExactSize() {
        var input = IntStream.range(0, 16).boxed().toList();
        assertEquals(16, DomainEntityScan.cap(input, 16).size());
    }
}
