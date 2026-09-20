package com.mpp.stellaeomphalos.constellation.starmap;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class StarGeometryTest {
    @Test void starPointValidatesGridBounds() {
        assertThrows(IllegalArgumentException.class, () -> new StarPoint(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new StarPoint(0, 31));
        assertThrows(IllegalArgumentException.class, () -> new StarPoint(31, 31));
        assertDoesNotThrow(() -> new StarPoint(0, 0));
        assertDoesNotThrow(() -> new StarPoint(30, 30));
    }

    @Test void manhattanDistanceToOrigin() {
        assertEquals(0, new StarPoint(0, 0).manhattan());
        assertEquals(25, new StarPoint(10, 15).manhattan());
        assertEquals(60, new StarPoint(30, 30).manhattan());
    }

    @Test void starLineIsUndirectedWithConsistentHashCode() {
        var a = new StarPoint(3, 4);
        var b = new StarPoint(10, 2);
        var forward = new StarLine(a, b);
        var backward = new StarLine(b, a);
        assertEquals(forward, backward);
        assertEquals(forward.hashCode(), backward.hashCode());
        assertEquals(forward.a(), backward.a());
        assertEquals(forward.b(), backward.b());
        assertTrue(forward.a().x() < forward.b().x() || forward.a().x() == forward.b().x() && forward.a().y() < forward.b().y());
    }

    @Test void starLineRejectsSelfConnection() {
        var a = new StarPoint(5, 5);
        assertThrows(IllegalArgumentException.class, () -> new StarLine(a, a));
    }

    @Test void signDrawnCarriesDrawSize() {
        var drawn = new SignDrawn(new ResourceLocation("stellaeomphalos", "aevitas"), 4, 9);
        assertEquals(30, SignDrawn.DRAW_SIZE);
        assertEquals(4, drawn.gridX());
        assertEquals(9, drawn.gridZ());
    }
}
