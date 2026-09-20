package com.mpp.stellaeomphalos.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.constellation.attribute.*;
import com.mpp.stellaeomphalos.player.mantle.*;

import net.minecraft.nbt.CompoundTag;

import org.junit.jupiter.api.Test;

class MantleContractsTest {
    static MantleParameters params() {
        return new MantleParameters(20, 3, .1, .6, .5, 200, 1000, .1);
    }

    @Test
    void allTwelveEffectsAreReadOnlyForEverySimulatedCallback() {
        assertEquals(12, MantleRegistry.ids().size());
        for (var id : MantleRegistry.ids()) {
            var data = new CompoundTag();
            data.putInt("Stacks", 2);
            data.putFloat("LastHit", 10);
            data.putLong("LastHitAt", 99);
            var effect = MantleRegistry.create(new MantleState(id, data), params());
            var before = effect.snapshot();
            for (var kind : MantleAction.Kind.values())
                for (int i = 0; i < 10; i++)
                    effect.perform(new MantleAction(kind, 100 + i, 12, true, true), true);
            assertEquals(before, effect.snapshot());
            assertEquals(data, before.data());
        }
    }

    @Test
    void guardianConsumesExactlyOneStackOnlyOnCommit() {
        var data = new CompoundTag();
        data.putInt("Stacks", 2);
        var effect =
                MantleRegistry.create(
                        new MantleState(MantleRegistry.id(MantleRegistry.Kind.GUARDIAN), data),
                        params());
        var action = new MantleAction(MantleAction.Kind.HURT, 1, 5, false, false);
        assertTrue(effect.perform(action, true).immune());
        assertEquals(2, effect.snapshot().data().getInt("Stacks"));
        assertTrue(effect.perform(action, false).immune());
        assertEquals(1, effect.snapshot().data().getInt("Stacks"));
    }

    @Test
    void stateMovesWithItemAndConfigurationDoesNotResetIt() {
        var initial = new CompoundTag();
        initial.putInt("Stacks", 2);
        initial.putInt("RechargeTicks", 19);
        var state = new MantleState(MantleRegistry.id(MantleRegistry.Kind.GUARDIAN), initial);
        var playerA = MantleRegistry.create(state, params());
        var playerB =
                MantleRegistry.create(
                        playerA.snapshot(), new MantleParameters(1, 5, .2, .3, 1, 300, 2000, .2));
        assertEquals(playerA.snapshot(), playerB.snapshot());
        playerB.perform(new MantleAction(MantleAction.Kind.TICK, 1, 0, false, false), false);
        assertEquals(3, playerB.snapshot().data().getInt("Stacks"));
        assertEquals(2, playerA.snapshot().data().getInt("Stacks"));
    }

    @Test
    void retortExpiresAndFireConversionConservesDamage() {
        var effect =
                MantleRegistry.create(
                        new MantleState(
                                MantleRegistry.id(MantleRegistry.Kind.RETORT), new CompoundTag()),
                        params());
        effect.perform(new MantleAction(MantleAction.Kind.HURT, 100, 10, false, false), false);
        assertEquals(
                7,
                effect.perform(
                                new MantleAction(MantleAction.Kind.ATTACK, 200, 2, false, false),
                                true)
                        .damage());
        assertEquals(
                2,
                effect.perform(
                                new MantleAction(MantleAction.Kind.ATTACK, 301, 2, false, false),
                                true)
                        .damage());
        var fire =
                MantleRegistry.create(
                        new MantleState(
                                MantleRegistry.id(MantleRegistry.Kind.HEARTH), new CompoundTag()),
                        params());
        var result =
                fire.perform(new MantleAction(MantleAction.Kind.HURT, 0, 10, true, false), true);
        assertEquals(10, result.damage() + result.healing(), .00001);
    }

    @Test
    void gaugeBreakdownAndPostProcessingSuffixAreNumericallyConsistent() {
        var reading = GaugeReading.evaluate("x", 5, 2, .5, 2, 21, "");
        assertEquals(21, reading.breakdown().computed());
        assertEquals("", reading.breakdown().note());
        assertEquals("", GaugeReading.evaluate("x", 5, 2, .5, 2, 21.00001, "").breakdown().note());
        assertFalse(GaugeReading.evaluate("x", 5, 2, .5, 2, 20, "").breakdown().note().isEmpty());
    }

    @Test
    void harvestSamplingFinallyRestoresItsGuardOnNestedFailures() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        BoonAttributeListeners.withoutHarvestSpeedBonus(
                                () -> {
                                    throw new IllegalStateException("injected");
                                }));
        assertEquals(
                7,
                BoonAttributeListeners.withoutHarvestSpeedBonus(
                        () -> BoonAttributeListeners.withoutHarvestSpeedBonus(() -> 7)));
    }
}
