package com.mpp.stellaeomphalos.content.item.amulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * 附魔护符的单条修正（《方块物品实体完整清单》§6.3.5 / §6.4.4）。
 *
 * <p>三类语义：`ADD_SPECIFIC` 给指定附魔叠加等级；`ADD_TO_EXISTING_SPECIFIC` 只在该附魔已存在时叠加；
 * `ADD_TO_EXISTING_ALL` 对**全部**已存在附魔叠加（全局修正，上限 {@link #GLOBAL_MODIFIER_LIMIT} 次）。
 * 原模组的 `DynamicEnchantment.Type` 由本枚举取代，并整体 `Codec` 化以便与 NBT / 数据包共用同一 schema。
 */
public record AmuletModifier(
        ModifierKind kind, Optional<ResourceLocation> enchantment, int levelDelta) {

    /** `ADD_TO_EXISTING_ALL` 的最大次数（§6.4.4：硬编码常量，不开放配置）。 */
    public static final int GLOBAL_MODIFIER_LIMIT = 2;

    /** 单条修正的最大等级增量。 */
    public static final int MAX_LEVEL_DELTA = 3;

    public static final Codec<AmuletModifier> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            ModifierKind.CODEC
                                                    .fieldOf("kind")
                                                    .forGetter(AmuletModifier::kind),
                                            ResourceLocation.CODEC
                                                    .optionalFieldOf("enchantment")
                                                    .forGetter(AmuletModifier::enchantment),
                                            Codec.intRange(1, MAX_LEVEL_DELTA)
                                                    .fieldOf("level_delta")
                                                    .forGetter(AmuletModifier::levelDelta))
                                    .apply(instance, AmuletModifier::new));

    /** 修正类型。 */
    public enum ModifierKind implements StringRepresentable {
        /** 无条件给指定附魔叠加。 */
        ADD_SPECIFIC,
        /** 仅当目标已带有该附魔时叠加。 */
        ADD_TO_EXISTING_SPECIFIC,
        /** 对全部已存在附魔叠加。 */
        ADD_TO_EXISTING_ALL;

        public static final Codec<ModifierKind> CODEC = StringRepresentable.fromEnum(ModifierKind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public boolean requiresEnchantment() {
            return this != ADD_TO_EXISTING_ALL;
        }
    }

    public AmuletModifier {
        levelDelta = Math.max(1, Math.min(MAX_LEVEL_DELTA, levelDelta));
        if (kind.requiresEnchantment() && enchantment.isEmpty())
            throw new IllegalArgumentException(kind + " requires a target enchantment");
        if (!kind.requiresEnchantment()) enchantment = Optional.empty();
    }

    /** 两条修正是否可以合并为一（同类 + 同目标附魔）。 */
    public boolean canMerge(AmuletModifier other) {
        return other != null && kind == other.kind && enchantment.equals(other.enchantment);
    }

    /** 合并同类项，等级增量相加并受 {@link #MAX_LEVEL_DELTA} 钳制。 */
    public AmuletModifier merge(AmuletModifier other) {
        if (!canMerge(other)) throw new IllegalArgumentException("Incompatible amulet modifiers");
        return new AmuletModifier(
                kind, enchantment, Math.min(MAX_LEVEL_DELTA, levelDelta + other.levelDelta));
    }

    /** 该修正是否作用于给定的（附魔，现有等级）组合。 */
    public boolean appliesTo(ResourceLocation candidate, int existingLevel) {
        return switch (kind) {
            case ADD_SPECIFIC -> enchantment.isPresent() && enchantment.get().equals(candidate);
            case ADD_TO_EXISTING_SPECIFIC ->
                    existingLevel > 0 && enchantment.isPresent() && enchantment.get().equals(candidate);
            case ADD_TO_EXISTING_ALL -> existingLevel > 0;
        };
    }

    /** 供 tooltip 与调试输出使用的可读形态。 */
    public String describe() {
        return switch (kind) {
            case ADD_SPECIFIC -> "+" + levelDelta + " " + enchantment.map(ResourceLocation::toString).orElse("?");
            case ADD_TO_EXISTING_SPECIFIC ->
                    "+" + levelDelta + " (existing) " + enchantment.map(ResourceLocation::toString).orElse("?");
            case ADD_TO_EXISTING_ALL -> "+" + levelDelta + " (all existing)";
        };
    }
}
