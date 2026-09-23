package com.mpp.stellaeomphalos.content.item.amulet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * 附魔护符的修正掷骰器（《方块物品实体完整清单》§6.3.5 / §6.4.4）。
 *
 * <p>原模组把 5 个概率写成类内常量，本项目全部改为可配置（{@link Probabilities#fromConfig()}），
 * 并把"最多 2 次全局修正"保留为硬编码常量。掷骰结果按同类项合并，最多 3 条修正。
 */
public final class AmuletRoller {

    private AmuletRoller() {}

    /** 掷骰概率集合；与 `OmphalosConfig.COMMON` 的 `amulet.*` 键一一对应。 */
    public record Probabilities(
            double secondRoll,
            double thirdRoll,
            double extraLevel,
            double globalModifier,
            double newEnchantment) {

        public static final Probabilities DEFAULTS = new Probabilities(0.8D, 0.25D, 0.15D, 0.02D, 0.35D);

        public Probabilities {
            secondRoll = clamp(secondRoll);
            thirdRoll = clamp(thirdRoll);
            extraLevel = clamp(extraLevel);
            globalModifier = clamp(globalModifier);
            newEnchantment = clamp(newEnchantment);
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }

        /** 从公共配置读取；配置不可用时退回 {@link #DEFAULTS}。 */
        public static Probabilities fromConfig() {
            try {
                var common = com.mpp.stellaeomphalos.OmphalosConfig.COMMON.snapshot();
                return new Probabilities(
                        number(common, "amulet.chanceSecondRoll", DEFAULTS.secondRoll()),
                        number(common, "amulet.chanceThirdRoll", DEFAULTS.thirdRoll()),
                        number(common, "amulet.chanceExtraLevel", DEFAULTS.extraLevel()),
                        number(common, "amulet.chanceGlobalModifier", DEFAULTS.globalModifier()),
                        number(common, "amulet.chanceNewEnchantment", DEFAULTS.newEnchantment()));
            } catch (RuntimeException exception) {
                return DEFAULTS;
            }
        }

        private static double number(Map<String, Object> values, String key, double fallback) {
            var value = values.get(key);
            return value instanceof Number number ? number.doubleValue() : fallback;
        }
    }

    /**
     * 掷出护符的修正列表。
     *
     * @param random 随机源（服务端为世界随机，测试可注入种子）
     * @param candidates 可作为目标的附魔 id（空表时返回空结果，不抛异常）
     * @param probabilities 概率集合
     * @param levelOfExisting 查询"该附魔在佩戴者身上是否已存在"的回调
     */
    public static List<AmuletModifier> roll(
            RandomSource random,
            List<ResourceLocation> candidates,
            Probabilities probabilities,
            java.util.function.ToIntFunction<ResourceLocation> levelOfExisting) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        var rolled = new ArrayList<AmuletModifier>();
        int rolls = 1;
        if (random.nextDouble() < probabilities.secondRoll()) rolls++;
        if (rolls == 2 && random.nextDouble() < probabilities.thirdRoll()) rolls++;

        int globalModifiers = 0;
        List<ResourceLocation> available = new ArrayList<>(candidates);
        for (int i = 0; i < rolls && !available.isEmpty(); i++) {
            var target = available.remove(random.nextInt(available.size()));
            int existing = levelOfExisting.applyAsInt(target);
            int delta = 1;
            if (random.nextDouble() < probabilities.extraLevel()) delta++;

            boolean global =
                    globalModifiers < AmuletModifier.GLOBAL_MODIFIER_LIMIT
                            && existing > 0
                            && random.nextDouble() < probabilities.globalModifier();
            if (global) {
                globalModifiers++;
                rolled.add(
                        new AmuletModifier(
                                AmuletModifier.ModifierKind.ADD_TO_EXISTING_ALL,
                                Optional.empty(),
                                delta));
                continue;
            }
            boolean newlyGranted = existing <= 0 && random.nextDouble() < probabilities.newEnchantment();
            var kind =
                    existing > 0
                            ? AmuletModifier.ModifierKind.ADD_TO_EXISTING_SPECIFIC
                            : (newlyGranted
                                    ? AmuletModifier.ModifierKind.ADD_SPECIFIC
                                    : AmuletModifier.ModifierKind.ADD_TO_EXISTING_SPECIFIC);
            rolled.add(new AmuletModifier(kind, Optional.of(target), delta));
        }
        return merge(rolled);
    }

    /** 合并同类项并保持插入顺序。 */
    public static List<AmuletModifier> merge(List<AmuletModifier> modifiers) {
        var merged = new LinkedHashMap<String, AmuletModifier>();
        for (var modifier : modifiers) {
            if (modifier == null) continue;
            var key = modifier.kind() + "|" + modifier.enchantment().map(ResourceLocation::toString).orElse("*");
            merged.merge(key, modifier, AmuletModifier::merge);
        }
        return List.copyOf(merged.values());
    }

    /**
     * 把修正列表应用到一次附魔等级查询上。
     *
     * @return 叠加后的等级（不修改入参映射）
     */
    public static int apply(
            List<AmuletModifier> modifiers, ResourceLocation enchantment, int baseLevel) {
        int level = baseLevel;
        for (var modifier : modifiers)
            if (modifier.appliesTo(enchantment, baseLevel)) level += modifier.levelDelta();
        return level;
    }

    /** 对整张等级表应用全部修正（供查询桥使用）。 */
    public static Map<ResourceLocation, Integer> applyAll(
            List<AmuletModifier> modifiers, Map<ResourceLocation, Integer> base) {
        var keys = new java.util.LinkedHashSet<ResourceLocation>(base.keySet());
        for (var modifier : modifiers) modifier.enchantment().ifPresent(keys::add);
        var result = new LinkedHashMap<ResourceLocation, Integer>();
        for (var key : keys) result.put(key, apply(modifiers, key, base.getOrDefault(key, 0)));
        return result;
    }
}
