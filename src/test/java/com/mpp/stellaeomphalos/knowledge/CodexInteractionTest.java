package com.mpp.stellaeomphalos.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.client.codex.*;
import com.mpp.stellaeomphalos.knowledge.codex.CodexRoute;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.*;

class CodexInteractionTest {
    @Test
    void structureRotationStartsAfterThirtyFramesAndLayoutIsReplaced() {
        var state = new CodexPageState();
        for (int i = 0; i < 30; i++) {
            state.frame();
            assertFalse(state.rotate(7));
        }
        assertEquals(0, state.rotation());
        state.frame();
        assertTrue(state.rotate(7));
        assertEquals(7, state.rotation());
        for (int i = 0; i < 3; i++) {
            state.layout(List.of("title", "body"));
            assertEquals(2, state.lines().size());
        }
    }

    @Test
    void ingredientCarouselHasExactTwentyTickBoundaries() {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                for (int tick = 0; tick < 100; tick++)
                    assertEquals(
                            (tick + row * 40 + col * 40) / 20 % 3,
                            CodexPageState.candidate(tick, row, col, 3));
        assertEquals(-1, CodexPageState.candidate(0, 0, 0, 0));
    }

    @Test
    void viewportClampsAtEveryZoomAndUnfocusedZoomStopsAtFour() {
        var canvas = new StudyCanvas();
        canvas.bounds(512, 512, 320, 220);
        canvas.zoom(100, 4);
        assertEquals(4, canvas.scale());
        canvas.focus(new StudyCanvas.Point(100, -50));
        for (int i = 0; i < 20; i++) {
            canvas.zoom(.5, 4);
            canvas.centerStep(4, 6);
            canvas.pan(100000, -100000);
            assertTrue(Math.abs(canvas.center().x()) <= (512 - 320 / canvas.scale()) / 2 + .0001);
            assertTrue(Math.abs(canvas.center().y()) <= (512 - 220 / canvas.scale()) / 2 + .0001);
        }
        assertEquals(10, canvas.scale());
        assertFalse(StudyCanvas.clickable(.699, .7));
        assertTrue(StudyCanvas.clickable(.7, .7));
    }

    @Test
    void doubleClickRequiresSameTargetAndFourHundredMilliseconds() {
        var canvas = new StudyCanvas();
        var focus = new StudyCanvas.Point(20, 30);
        assertFalse(canvas.doubleClick("a", 1000, focus));
        assertFalse(canvas.doubleClick("b", 1100, focus));
        assertFalse(canvas.doubleClick("b", 1501, focus));
        assertTrue(canvas.doubleClick("b", 1901, focus));
        assertEquals(9.9, canvas.scale());
        canvas.reset();
        assertEquals(1, canvas.scale());
        assertEquals(new StudyCanvas.Point(0, 0), canvas.center());
    }

    @Test
    void boundedHistoryHasIndependentBackAndForwardPaths() {
        var navigator = new CodexNavigator();
        for (int i = 0; i < 100; i++)
            navigator.push(new CodexRoute(new ResourceLocation("stellaeomphalos:n" + i), 0));
        assertEquals(64, navigator.historySize());
        var finalRoute = navigator.current();
        navigator.back();
        assertEquals(finalRoute, navigator.forward());
        navigator.back();
        navigator.push(new CodexRoute(new ResourceLocation("stellaeomphalos:new"), 0));
        assertEquals(navigator.current(), navigator.forward());
        navigator.reset();
        assertTrue(navigator.current().isEmpty());
        assertTrue(CodexRoute.parse("bad").isEmpty());
        assertTrue(CodexRoute.parse("stellaeomphalos:x#-1").isEmpty());
    }

    @Test
    void ribbonOrdersAreUnique() {
        var registry = CodexRibbonRegistry.defaults();
        assertEquals(6, registry.all().size());
        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(new CodexRibbonRegistry.Ribbon(10, "duplicate")));
    }
}
