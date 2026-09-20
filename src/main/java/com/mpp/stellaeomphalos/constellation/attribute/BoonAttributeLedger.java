package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;

/** 三段式合成：base → ΣADDITION → entered×ΣADDED_MULTIPLY×potency → 连乘 ((m−1)×potency+1) → clamp。 */
public final class BoonAttributeLedger {

    private BoonAttributeLedger() {}

    public static double compose(BoonAttribute attribute, List<? extends BoonModifier> modifiers, double potency) {
        double value = attribute.defaultValue() + aggregate(modifiers, BoonModifier.Mode.ADDITION, 1.0);
        double entered = value;
        value += entered * aggregate(modifiers, BoonModifier.Mode.ADDED_MULTIPLY, potency);
        value *= aggregate(modifiers, BoonModifier.Mode.STACKING_MULTIPLY, potency);
        if (Double.isNaN(value)) value = attribute.defaultValue();
        return BoonAttributeClampRegistry.clampFor(attribute).clamp(value);
    }

    /** 单模式累计修量；ADDITION 不吃 potency，ADDED_MULTIPLY 线性吃，STACKING_MULTIPLY 返回连乘总因子。 */
    public static double aggregate(List<? extends BoonModifier> modifiers, BoonModifier.Mode mode, double potency) {
        return switch (mode) {
            case ADDITION -> {
                double sum = 0.0;
                for (var modifier : modifiers) if (modifier.mode() == mode) sum += modifier.value();
                yield sum;
            }
            case ADDED_MULTIPLY -> {
                double sum = 0.0;
                for (var modifier : modifiers) if (modifier.mode() == mode) sum += modifier.value();
                yield sum * potency;
            }
            case STACKING_MULTIPLY -> {
                double product = 1.0;
                for (var modifier : modifiers)
                    if (modifier.mode() == mode) product *= (modifier.value() - 1.0) * potency + 1.0;
                yield product;
            }
        };
    }
}
