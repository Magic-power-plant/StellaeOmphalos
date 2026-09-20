package com.mpp.stellaeomphalos.crafting;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.crafting.infusion.InfusionTask;
import com.mpp.stellaeomphalos.data.codec.NbtSubset;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class CraftLifecycleTest {
    private AsterismCraftTask task(int duration) {
        return new AsterismCraftTask(
                new ResourceLocation("test", "craft"), 1, duration, new UUID(1, 2));
    }

    @Test
    void fiveTiersMatchContract() {
        assertEquals(5, AsterismTier.values().length);
        int[] visible = {9, 13, 21, 25, 25};
        long[] caps = {1000, 2000, 4000, 8000, 16000};
        for (int i = 0; i < 5; i++) {
            var tier = AsterismTier.values()[i];
            assertEquals(25, tier.accessibleSlotCount());
            assertEquals(visible[i], tier.visibleSlotCount());
            assertEquals(caps[i], tier.lumenCapacity());
            assertEquals(1, tier.maxConcurrentCrafts());
        }
        assertFalse(AsterismTier.RADIANCE.requiresStructure());
    }

    @Test
    void noCommitBeforeFinishAndNoDuplicateCommit() {
        var task = task(2);
        int[] commits = {0};
        assertFalse(
                task.commit(
                        () -> {
                            commits[0]++;
                            return true;
                        }));
        task.advance(true, true, true, true, true, true);
        assertEquals(CraftState.RUNNING, task.state());
        task.advance(true, true, true, true, true, true);
        assertTrue(
                task.commit(
                        () -> {
                            commits[0]++;
                            return true;
                        }));
        assertFalse(
                task.commit(
                        () -> {
                            commits[0]++;
                            return true;
                        }));
        assertEquals(1, commits[0]);
    }

    @Test
    void takingMaterialsAbortsWithoutFinishing() {
        var task = task(2);
        task.advance(true, true, true, true, true, true);
        task.advance(true, false, true, true, true, true);
        assertEquals(CraftState.IDLE, task.state());
        assertFalse(task.commit(() -> fail("Must not consume")));
    }

    @Test
    void energyPausePreservesProgress() {
        var task = task(100);
        task.advance(true, true, true, true, true, true);
        for (int i = 0; i < 300; i++) task.advance(true, true, true, false, true, true);
        assertEquals(1, task.ticks());
        assertEquals(CraftState.PAUSED, task.state());
        task.advance(true, true, true, true, true, true);
        assertEquals(2, task.ticks());
    }

    @Test
    void structureRecoversWithinGraceAndAbortsAfterGrace() {
        var task = task(100);
        task.advance(true, true, true, true, true, true);
        for (int i = 0; i < 199; i++) task.advance(true, true, false, true, true, true);
        assertEquals(CraftState.SUSPENDED, task.state());
        task.advance(true, true, true, true, true, true);
        assertEquals(2, task.ticks());
        for (int i = 0; i < 200; i++) task.advance(true, true, false, true, true, true);
        assertEquals(CraftState.IDLE, task.state());
    }

    @Test
    void orphanExpiresWithoutConsuming() {
        var task = task(20);
        for (int i = 0; i < 199; i++) task.advance(false, true, true, true, true, true);
        assertEquals(CraftState.ORPHANED, task.state());
        task.advance(false, true, true, true, true, true);
        assertEquals(CraftState.IDLE, task.state());
    }

    @Test
    void reloadRescalesAndRestoresOrphan() {
        var task = task(100);
        for (int i = 0; i < 25; i++) task.advance(true, true, true, true, true, true);
        task.advance(false, true, true, true, true, true);
        task.reconcile(2, 200);
        assertEquals(50, task.ticks());
        assertEquals(CraftState.RUNNING, task.state());
    }

    @Test
    void saveRoundTripRetainsRecipeProgressRelayAndPrivateData() {
        var task = task(100);
        task.advance(true, true, true, true, true, true);
        task.bind(3, new net.minecraft.core.BlockPos(5, 60, 7));
        var data = new CompoundTag();
        data.putInt("Phase", 2);
        task.recipeData(data);
        var loaded = AsterismCraftTask.load(task.save()).orElseThrow();
        assertEquals(task.recipeId(), loaded.recipeId());
        assertEquals(task.crafter(), loaded.crafter());
        assertEquals(1, loaded.ticks());
        assertEquals(task.bindings(), loaded.bindings());
        assertEquals(data, loaded.recipeData());
    }

    @Test
    void invalidRecipeIdDiscardsOnlyTask() {
        var tag = new CompoundTag();
        tag.putString("Recipe", "INVALID ID");
        assertTrue(AsterismCraftTask.load(tag).isEmpty());
    }

    @Test
    void infusionCoordinatesAreBoundedAndPersistent() {
        var task = new InfusionTask(new ResourceLocation("test", "infuse"), 3, 40, new UUID(0, 1));
        task.solventPositions(java.util.List.of(new net.minecraft.core.BlockPos(4, 5, 6)));
        var loaded = InfusionTask.load(task.save()).orElseThrow();
        assertEquals(task.solventPositions(), loaded.solventPositions());
    }

    @Test
    void fixedPointDecayAndSkyThreshold() {
        var noSky = AsterismEnergy.passive(1000, 0, 16000, false, 100, 1, 1);
        assertEquals(950, noSky.stored());
        assertEquals(950, AsterismEnergy.passive(1000, 0, 16000, true, 40, 1, 1).stored());
        assertEquals(1110, AsterismEnergy.passive(1000, 0, 16000, true, 120, 1, 1).stored());
        assertEquals(1000, AsterismEnergy.passive(1000, 0, 1000, true, 120, 1, 1).stored());
    }

    @Test
    void lowNoiseAndDaytimeRetainTheirDocumentedBaseline() {
        assertEquals(19, AsterismEnergy.passive(0, 0, 16000, true, 120, 0, 0).stored());
        assertEquals(200, AsterismEnergy.NETWORK_MULTIPLIER);
    }

    @Test
    void fractionalCarryDoesNotVanish() {
        var balance = AsterismEnergy.passive(0, 0, 16000, true, 41, 0.1, 0.1);
        assertTrue(balance.carry() > 0);
        assertEquals(0, balance.stored());
    }

    @Test
    void nbtListsUseMultisetContainment() throws Exception {
        assertFalse(
                NbtSubset.contains(
                        TagParser.parseTag("{L:[{V:1}]}"),
                        TagParser.parseTag("{L:[{V:1},{V:1}]}")));
        assertTrue(
                NbtSubset.contains(
                        TagParser.parseTag("{L:[{V:1,Extra:2},{V:2}],X:9}"),
                        TagParser.parseTag("{L:[{V:2},{V:1}]}")));
    }

    @Test
    void overlappingNbtSubsetsUseCompleteMatching() throws Exception {
        assertTrue(
                NbtSubset.contains(
                        TagParser.parseTag("{L:[{V:1,Extra:2},{V:1}]}"),
                        TagParser.parseTag("{L:[{V:1},{V:1,Extra:2}]}")));
    }

    @Test
    void allTierSlotsStayClearOfControlsAndInventory() {
        for (var tier : AsterismTier.values())
            for (int index = 0; index < tier.visibleSlotCount(); index++) {
                int x = com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout.slotX(index);
                int y = com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout.slotY(index);
                assertTrue(
                        x >= 0
                                && x + 16
                                        < com.mpp.stellaeomphalos.crafting.altar.menu
                                                .AsterismMenuLayout.WIDTH);
                assertTrue(
                        y + 16
                                < com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout
                                        .BUTTON_Y);
                for (int other = 0; other < index; other++) {
                    int dx =
                            Math.abs(
                                    x
                                            - com.mpp.stellaeomphalos.crafting.altar.menu
                                                    .AsterismMenuLayout.slotX(other));
                    int dy =
                            Math.abs(
                                    y
                                            - com.mpp.stellaeomphalos.crafting.altar.menu
                                                    .AsterismMenuLayout.slotY(other));
                    assertTrue(dx >= 18 || dy >= 18, "Ingredient slots overlap");
                }
            }
        assertTrue(
                com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout.BUTTON_Y + 16
                        < com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout.PLAYER_Y);
        assertTrue(
                com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout.HOTBAR_Y + 18
                        < com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout.HEIGHT);
    }
}
