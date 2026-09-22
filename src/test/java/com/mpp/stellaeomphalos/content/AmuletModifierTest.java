package com.mpp.stellaeomphalos.content;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.content.item.amulet.AmuletEnchantBridge;
import com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder;
import com.mpp.stellaeomphalos.content.item.amulet.AmuletModifier;
import com.mpp.stellaeomphalos.content.item.amulet.AmuletModifier.ModifierKind;
import com.mpp.stellaeomphalos.content.item.amulet.AmuletRoller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Part-6 §6.3.5 / §6.4.4 护符动态附魔修正的纯 JVM 验收。
 *
 * <p>覆盖：Codec 往返、同类项合并与全局修正上限、三类语义的适用判定、
 * 掷骰的确定性与概率边界、以及上下文桥的应用与清理。
 */
class AmuletModifierTest {

    private static final ResourceLocation SHARPNESS = new ResourceLocation("minecraft", "sharpness");
    private static final ResourceLocation UNBREAKING = new ResourceLocation("minecraft", "unbreaking");

    @BeforeAll
    static void bootstrap() {
        try {
            var guard = net.minecraft.server.Bootstrap.class.getDeclaredField("isBootstrapped");
            guard.setAccessible(true);
            if (!guard.getBoolean(null)) {
                SharedConstants.tryDetectVersion();
                guard.setBoolean(null, true);
                net.minecraft.core.registries.BuiltInRegistries.bootStrap();
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void modifier_requires_a_target_unless_global() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.empty(), 1));
        var global =
                new AmuletModifier(ModifierKind.ADD_TO_EXISTING_ALL, Optional.of(SHARPNESS), 1);
        assertTrue(global.enchantment().isEmpty(), "Global modifiers drop the target");
    }

    @Test
    void level_delta_is_clamped_to_the_representable_range() {
        assertEquals(
                1,
                new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 0)
                        .levelDelta());
        assertEquals(
                AmuletModifier.MAX_LEVEL_DELTA,
                new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 99)
                        .levelDelta());
    }

    @Test
    void merge_combines_same_kind_and_clamps() {
        var first = new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 1);
        var second = new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 2);
        assertTrue(first.canMerge(second));
        assertEquals(3, first.merge(second).levelDelta());
        assertEquals(
                AmuletModifier.MAX_LEVEL_DELTA,
                first.merge(second).merge(second).levelDelta(),
                "Merging must clamp to MAX_LEVEL_DELTA");
        var other = new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(UNBREAKING), 1);
        assertFalse(first.canMerge(other));
        assertThrows(IllegalArgumentException.class, () -> first.merge(other));
    }

    @Test
    void applies_to_respects_the_three_kinds() {
        var specific = new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 1);
        assertTrue(specific.appliesTo(SHARPNESS, 0), "ADD_SPECIFIC works without the base level");
        assertFalse(specific.appliesTo(UNBREAKING, 3));

        var existing =
                new AmuletModifier(ModifierKind.ADD_TO_EXISTING_SPECIFIC, Optional.of(SHARPNESS), 1);
        assertFalse(existing.appliesTo(SHARPNESS, 0));
        assertTrue(existing.appliesTo(SHARPNESS, 2));

        var global = new AmuletModifier(ModifierKind.ADD_TO_EXISTING_ALL, Optional.empty(), 1);
        assertFalse(global.appliesTo(SHARPNESS, 0));
        assertTrue(global.appliesTo(UNBREAKING, 1));
    }

    @Test
    void roll_is_deterministic_for_a_seed_and_merges_duplicates() {
        var candidates = List.of(SHARPNESS, UNBREAKING);
        var probabilities = new AmuletRoller.Probabilities(1.0D, 1.0D, 0.0D, 0.0D, 1.0D);
        var first =
                AmuletRoller.roll(
                        net.minecraft.util.RandomSource.create(99L),
                        candidates,
                        probabilities,
                        id -> 0);
        var second =
                AmuletRoller.roll(
                        net.minecraft.util.RandomSource.create(99L),
                        candidates,
                        probabilities,
                        id -> 0);
        assertEquals(first, second, "Same seed must produce the same roll");
        assertFalse(first.isEmpty(), "All-chance probabilities must yield modifiers");
        // 合并后同类项唯一：不允许出现两条同 kind 同目标的修正。
        for (int i = 0; i < first.size(); i++)
            for (int j = i + 1; j < first.size(); j++)
                assertFalse(first.get(i).canMerge(first.get(j)), "Duplicates must be merged");
    }

    @Test
    void roll_without_candidates_is_empty_and_safe() {
        assertTrue(
                AmuletRoller.roll(
                                net.minecraft.util.RandomSource.create(1L),
                                List.of(),
                                AmuletRoller.Probabilities.DEFAULTS,
                                id -> 0)
                        .isEmpty());
    }

    @Test
    void global_modifier_count_is_capped() {
        var probabilities = new AmuletRoller.Probabilities(1.0D, 1.0D, 0.0D, 1.0D, 0.0D);
        var candidates = List.of(SHARPNESS, UNBREAKING, new ResourceLocation("minecraft", "mending"));
        var modifiers =
                AmuletRoller.roll(
                        net.minecraft.util.RandomSource.create(5L),
                        candidates,
                        probabilities,
                        id -> 1);
        long globals =
                modifiers.stream()
                        .filter(m -> m.kind() == ModifierKind.ADD_TO_EXISTING_ALL)
                        .count();
        assertTrue(
                globals <= AmuletModifier.GLOBAL_MODIFIER_LIMIT,
                "Global modifiers must respect the hard limit, got " + globals);
    }

    @Test
    void apply_all_keeps_untouched_entries() {
        var modifiers =
                List.of(new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 2));
        var base = Map.of(SHARPNESS, 1, UNBREAKING, 3);
        var applied = AmuletRoller.applyAll(modifiers, base);
        assertEquals(3, applied.get(SHARPNESS));
        assertEquals(3, applied.get(UNBREAKING), "Unrelated entries must be unchanged");
        assertEquals(1, base.get(SHARPNESS), "The input map must not be mutated");
    }

    @Test
    void holder_round_trips_through_nbt() {
        var stack = new ItemStack(Items.PAPER);
        assertTrue(AmuletHolder.modifiers(stack).isEmpty(), "Fresh amulet has no modifiers");
        assertEquals(AmuletHolder.Data.empty(), AmuletHolder.read(stack));

        var modifiers =
                List.of(
                        new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 1),
                        new AmuletModifier(ModifierKind.ADD_TO_EXISTING_ALL, Optional.empty(), 2));
        AmuletHolder.setModifiers(stack, modifiers);
        var read = AmuletHolder.read(stack);
        assertTrue(read.rolled(), "Explicit write marks the amulet as rolled");
        assertEquals(modifiers, read.modifiers());
        assertEquals(2, AmuletHolder.describe(stack).getInt("Count"));
    }

    @Test
    void roll_if_needed_is_idempotent() {
        var stack = new ItemStack(Items.PAPER);
        var candidates = List.of(SHARPNESS);
        var probabilities = AmuletRoller.Probabilities.DEFAULTS;
        assertNotNull(probabilities);
        var first =
                AmuletHolder.rollIfNeeded(
                        stack,
                        net.minecraft.util.RandomSource.create(7L),
                        candidates,
                        id -> 0,
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertTrue(first, "First call must roll");
        var snapshot = AmuletHolder.read(stack);
        var second =
                AmuletHolder.rollIfNeeded(
                        stack,
                        net.minecraft.util.RandomSource.create(7L),
                        candidates,
                        id -> 0,
                        null);
        assertFalse(second, "Second call must be a no-op");
        assertEquals(snapshot, AmuletHolder.read(stack), "A rolled amulet never re-rolls");
    }

    @Test
    void bridge_context_is_scoped_and_cleared() {
        assertTrue(AmuletEnchantBridge.activeModifiers().isEmpty(), "No context by default");
        var modifiers =
                List.of(new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 1));
        try {
            AmuletEnchantBridge.pushContext(modifiers);
            assertEquals(modifiers, AmuletEnchantBridge.activeModifiers());
        } finally {
            AmuletEnchantBridge.clearContext();
        }
        assertTrue(
                AmuletEnchantBridge.activeModifiers().isEmpty(),
                "Context must be cleared to avoid leaking one player's amulet to another");
    }

    @Test
    void bridge_preview_matches_the_direct_application() {
        var modifiers =
                List.of(new AmuletModifier(ModifierKind.ADD_SPECIFIC, Optional.of(SHARPNESS), 2));
        AmuletEnchantBridge.pushContext(modifiers);
        try {
            var base = Map.of(SHARPNESS, 1);
            assertEquals(
                    AmuletRoller.applyAll(modifiers, base),
                    AmuletRoller.applyAll(AmuletEnchantBridge.activeModifiers(), base));
        } finally {
            AmuletEnchantBridge.clearContext();
        }
    }
}
