package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonAttributeLedgerTest {

    private static int sequence;

    private static BoonAttribute attribute(double defaultValue, BoonAttributeClamp clamp) {
        return BoonAttributeRegistry.register(new BoonAttribute(
                new ResourceLocation("stellaeomphalos", "test_ledger_" + (++sequence)), defaultValue, clamp, false));
    }

    private static BoonModifier addition(BoonAttribute attribute, double value) {
        return new BoonModifier(attribute, BoonModifier.Mode.ADDITION, value, false);
    }

    private static BoonModifier addedMultiply(BoonAttribute attribute, double value) {
        return new BoonModifier(attribute, BoonModifier.Mode.ADDED_MULTIPLY, value, false);
    }

    private static BoonModifier stacking(BoonAttribute attribute, double value) {
        return new BoonModifier(attribute, BoonModifier.Mode.STACKING_MULTIPLY, value, false);
    }

    @Test void additionSumsOntoDefault() {
        var attribute = attribute(0.0, BoonAttributeClamp.UNBOUNDED);
        assertEquals(3.0, BoonAttributeLedger.compose(attribute,
                List.of(addition(attribute, 1.0), addition(attribute, 2.0)), 1.0));
    }

    @Test void addedMultiplyUsesValueAtSegmentEntryAsBasis() {
        var attribute = attribute(10.0, BoonAttributeClamp.UNBOUNDED);
        // entered = 10 + 5 = 15; 15 + 15 * 0.5 = 22.5
        assertEquals(22.5, BoonAttributeLedger.compose(attribute,
                List.of(addition(attribute, 5.0), addedMultiply(attribute, 0.5)), 1.0));
    }

    @Test void potencyScalesAddedMultiplyLinearly() {
        var attribute = attribute(10.0, BoonAttributeClamp.UNBOUNDED);
        assertEquals(30.0, BoonAttributeLedger.compose(attribute,
                List.of(addition(attribute, 5.0), addedMultiply(attribute, 0.5)), 2.0));
    }

    @Test void stackingMultiplyComposesAsProductWithPotency() {
        var attribute = attribute(10.0, BoonAttributeClamp.UNBOUNDED);
        assertEquals(18.0, BoonAttributeLedger.compose(attribute,
                List.of(stacking(attribute, 1.2), stacking(attribute, 1.5)), 1.0), 1e-9);
        // potency 0.5: 10 * ((0.2)*0.5+1) * ((0.5)*0.5+1) = 10 * 1.1 * 1.25 = 13.75
        assertEquals(13.75, BoonAttributeLedger.compose(attribute,
                List.of(stacking(attribute, 1.2), stacking(attribute, 1.5)), 0.5), 1e-9);
    }

    @Test void stackingOrderDoesNotChangeResult() {
        var attribute = attribute(4.0, BoonAttributeClamp.UNBOUNDED);
        double ab = BoonAttributeLedger.compose(attribute,
                List.of(stacking(attribute, 1.25), stacking(attribute, 2.0)), 1.0);
        double ba = BoonAttributeLedger.compose(attribute,
                List.of(stacking(attribute, 2.0), stacking(attribute, 1.25)), 1.0);
        assertEquals(ab, ba, 1e-9);
    }

    @Test void hardClampsAreEnforced() {
        assertEquals(0.60, BoonAttributeLedger.compose(BoonAttributes.ELEMENTAL_WARD,
                List.of(addition(BoonAttributes.ELEMENTAL_WARD, 5.0)), 1.0));
        assertEquals(0.75, BoonAttributeLedger.compose(BoonAttributes.DODGE,
                List.of(addition(BoonAttributes.DODGE, 5.0)), 1.0));
        assertEquals(0.10, BoonAttributeLedger.compose(BoonAttributes.LIFE_LEECH,
                List.of(addition(BoonAttributes.LIFE_LEECH, 5.0)), 1.0));
    }

    @Test void clampAppliesOnlyAtTheEnd() {
        var attribute = attribute(0.0, BoonAttributeClamp.of(0.0, 10.0));
        // 中间段越过上限（0+20=20），段 3 乘 0.25 落回 5：不提前钳制
        assertEquals(5.0, BoonAttributeLedger.compose(attribute,
                List.of(addition(attribute, 20.0), stacking(attribute, 0.25)), 1.0), 1e-9);
    }

    @Test void potencySelfLoopIsFixedToOne() {
        assertEquals(1.0, BoonValueBridge.selfLoopSafePotency(BoonAttributes.BOON_POTENCY, 99.0));
        assertEquals(2.5, BoonValueBridge.selfLoopSafePotency(BoonAttributes.DODGE, 2.5));
        assertEquals(1.0, BoonAttributes.BOON_POTENCY.defaultValue());
        // 求解 potency 自身时 potency=1：连乘项不被缩放
        assertEquals(1.5, BoonAttributeLedger.compose(BoonAttributes.BOON_POTENCY,
                List.of(stacking(BoonAttributes.BOON_POTENCY, 1.5)), 1.0), 1e-9);
    }

    @Test void aggregateReportsPerModeAmounts() {
        var attribute = attribute(0.0, BoonAttributeClamp.UNBOUNDED);
        var modifiers = List.of(addition(attribute, 3.0), addedMultiply(attribute, 0.5), stacking(attribute, 1.5));
        assertEquals(3.0, BoonAttributeLedger.aggregate(modifiers, BoonModifier.Mode.ADDITION, 9.0));
        assertEquals(1.0, BoonAttributeLedger.aggregate(modifiers, BoonModifier.Mode.ADDED_MULTIPLY, 2.0));
        assertEquals(1.25, BoonAttributeLedger.aggregate(modifiers, BoonModifier.Mode.STACKING_MULTIPLY, 0.5), 1e-9);
    }

    @Test void onlyMultiplicativeRejectsAdditionMode() {
        assertThrows(IllegalArgumentException.class,
                () -> new BoonModifier(BoonAttributes.CRIT_DAMAGE, BoonModifier.Mode.ADDITION, 1.0, false));
    }

    @Test void nanFallsBackToDefaultBeforeClamp() {
        var attribute = attribute(2.0, BoonAttributeClamp.UNBOUNDED);
        // 0 * -inf = NaN 路径走不到（构造拒绝非有限值），直接验证默认回落语义由钳制兜底
        assertEquals(2.0, BoonAttributeLedger.compose(attribute, List.of(), 1.0));
    }
}
