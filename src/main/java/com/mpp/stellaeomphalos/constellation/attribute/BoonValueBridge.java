package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;

/** 跨层桥：player/boon 在装配期注册 Provider；constellation 侧经此取值，不 import player。 */
public final class BoonValueBridge {

    public interface Provider {
        List<BoonModifier> modifiersOf(Player player);

        List<BoonTranslator> translatorsOf(Player player);
    }

    private static final Provider EMPTY = new Provider() {
        @Override
        public List<BoonModifier> modifiersOf(Player player) {
            return List.of();
        }

        @Override
        public List<BoonTranslator> translatorsOf(Player player) {
            return List.of();
        }
    };

    private static volatile Provider provider = EMPTY;
    private static final BoonValueCache CACHE = new BoonValueCache();
    private static final List<Consumer<UUID>> INVALIDATION_LISTENERS = new CopyOnWriteArrayList<>();

    private BoonValueBridge() {}

    public static void registerProvider(Provider newProvider) {
        if (newProvider == null) throw new IllegalArgumentException("null provider");
        if (provider != EMPTY) throw new IllegalStateException("boon value provider already registered");
        provider = newProvider;
    }

    /** 单属性单模式的累计修量（含 boon_potency 与转译器链）；STACKING_MULTIPLY 返回连乘总因子。 */
    public static double resolve(Player player, BoonAttribute attribute, BoonModifier.Mode mode) {
        return CACHE.getOrCompute(new BoonValueCache.Key(player.getUUID(), attribute.id(), mode),
                () -> evaluate(player, attribute, mode));
    }

    /** 三段式合成后的最终值（含钳制）。 */
    public static double value(Player player, BoonAttribute attribute) {
        return CACHE.getOrCompute(new BoonValueCache.Key(player.getUUID(), attribute.id(), null), () -> {
            double value = attribute.defaultValue() + resolve(player, attribute, BoonModifier.Mode.ADDITION);
            double entered = value;
            value += entered * resolve(player, attribute, BoonModifier.Mode.ADDED_MULTIPLY);
            value *= resolve(player, attribute, BoonModifier.Mode.STACKING_MULTIPLY);
            if (Double.isNaN(value)) value = attribute.defaultValue();
            return BoonAttributeClampRegistry.clampFor(attribute).clamp(value);
        });
    }

    public static void invalidate(UUID playerId) {
        CACHE.invalidate(playerId);
        for (var listener : INVALIDATION_LISTENERS) listener.accept(playerId);
    }

    public static void invalidateAll() {
        CACHE.invalidateAll();
    }

    public static void addInvalidationListener(Consumer<UUID> listener) {
        INVALIDATION_LISTENERS.add(listener);
    }

    public static BoonValueCache cache() {
        return CACHE;
    }

    static double selfLoopSafePotency(BoonAttribute attribute, double resolvedPotency) {
        return attribute.id().equals(BoonAttributes.BOON_POTENCY.id()) ? 1.0 : resolvedPotency;
    }

    private static double evaluate(Player player, BoonAttribute attribute, BoonModifier.Mode mode) {
        boolean isPotency = attribute.id().equals(BoonAttributes.BOON_POTENCY.id());
        double potency = selfLoopSafePotency(attribute, isPotency ? 1.0 : value(player, BoonAttributes.BOON_POTENCY));
        var translated = applyTranslators(player, provider.modifiersOf(player), provider.translatorsOf(player));
        var matching = new ArrayList<BoonModifier>();
        for (var modifier : translated)
            if (modifier.attribute().id().equals(attribute.id()) && modifier.mode() == mode) matching.add(modifier);
        return BoonAttributeLedger.aggregate(matching, mode, potency);
    }

    /** absolute = true 的修饰符跳过全部转译（宝晶词条走此路径）。 */
    static List<BoonModifier> applyTranslators(Player player, List<BoonModifier> modifiers, List<BoonTranslator> translators) {
        if (translators.isEmpty()) return modifiers;
        var out = new ArrayList<BoonModifier>(modifiers.size());
        for (var modifier : modifiers) {
            if (modifier.absolute()) {
                out.add(modifier);
                continue;
            }
            List<BoonModifier> current = List.of(modifier);
            for (var translator : translators) {
                var next = new ArrayList<BoonModifier>();
                for (var item : current) {
                    var owner = item.ownerNode();
                    next.addAll(translator.translate(player, item, owner));
                }
                current = next;
            }
            out.addAll(current);
            for (var translator : translators) out.addAll(translator.extraModifiers(player, modifier));
        }
        return out;
    }
}
