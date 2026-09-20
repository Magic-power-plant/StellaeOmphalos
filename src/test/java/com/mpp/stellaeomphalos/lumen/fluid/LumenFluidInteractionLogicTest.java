package com.mpp.stellaeomphalos.lumen.fluid;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LumenFluidInteractionLogicTest {
    @Test
    void coldBranchAtOrBelowThreshold() {
        assertTrue(LumenFluidInteractions.isColdReaction(300));
        assertTrue(LumenFluidInteractions.isColdReaction(120));
        assertFalse(LumenFluidInteractions.isColdReaction(301));
        assertFalse(LumenFluidInteractions.isColdReaction(1300));
    }

    @Test
    void coldReactionYieldsConfiguredIce() {
        assertEquals(new ResourceLocation("minecraft", "ice"),
                LumenFluidInteractions.reactionResultId(300, LumenFluidInteractions.HotInteractionMode.SAND_AND_RARE,
                        "minecraft:ice", "minecraft:sand"));
    }

    @Test
    void hotReactionHonorsThreeStateMode() {
        assertNull(LumenFluidInteractions.reactionResultId(1300, LumenFluidInteractions.HotInteractionMode.OFF,
                "minecraft:ice", "minecraft:sand"));
        assertEquals(new ResourceLocation("minecraft", "sand"),
                LumenFluidInteractions.reactionResultId(1300, LumenFluidInteractions.HotInteractionMode.SAND,
                        "minecraft:ice", "minecraft:sand"));
        assertEquals(new ResourceLocation("minecraft", "sand"),
                LumenFluidInteractions.reactionResultId(1300, LumenFluidInteractions.HotInteractionMode.SAND_AND_RARE,
                        "minecraft:ice", "minecraft:sand"));
    }

    @Test
    void hotModeParsing() {
        assertEquals(LumenFluidInteractions.HotInteractionMode.OFF, LumenFluidInteractions.HotInteractionMode.parse("off"));
        assertEquals(LumenFluidInteractions.HotInteractionMode.SAND, LumenFluidInteractions.HotInteractionMode.parse("sand"));
        assertEquals(LumenFluidInteractions.HotInteractionMode.SAND_AND_RARE,
                LumenFluidInteractions.HotInteractionMode.parse("sand_and_rare"));
        assertEquals(LumenFluidInteractions.HotInteractionMode.SAND_AND_RARE,
                LumenFluidInteractions.HotInteractionMode.parse("SAND_AND_RARE"));
        assertEquals(LumenFluidInteractions.HotInteractionMode.SAND_AND_RARE,
                LumenFluidInteractions.HotInteractionMode.parse(null));
        assertEquals(LumenFluidInteractions.HotInteractionMode.SAND_AND_RARE,
                LumenFluidInteractions.HotInteractionMode.parse("garbage"));
    }

    @Test
    void rareDropRoll() {
        assertTrue(LumenFluidInteractions.rollsRareDrop(0));
        assertFalse(LumenFluidInteractions.rollsRareDrop(1));
        assertFalse(LumenFluidInteractions.rollsRareDrop(899));
    }

    @Test
    void logConversionConsumesOnePerTouch() {
        assertEquals(new LumenFluidInteractions.LogConversion(4, 1), LumenFluidInteractions.logConversion(5));
        assertEquals(new LumenFluidInteractions.LogConversion(0, 1), LumenFluidInteractions.logConversion(1));
        assertEquals(new LumenFluidInteractions.LogConversion(0, 0), LumenFluidInteractions.logConversion(0));
    }
}
