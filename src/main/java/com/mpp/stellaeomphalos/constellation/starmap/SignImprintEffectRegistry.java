package com.mpp.stellaeomphalos.constellation.starmap;

import com.mpp.stellaeomphalos.constellation.sign.Sign;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.enchantment.Enchantment;

/** Sign -> (enchantment pool, mob-effect pool) for starmap imprints. Duplicate registration throws. */
public final class SignImprintEffectRegistry {
    public record AffixEntry(Holder<Enchantment> enchantment, int minLevel, int maxLevel, boolean ignoreCompatibility) {
        public AffixEntry {
            if (minLevel < 1 || maxLevel < minLevel) throw new IllegalArgumentException("Invalid affix level range " + minLevel + ".." + maxLevel);
        }
    }
    public record PotionEntry(Holder<MobEffect> effect, int minLevel, int maxLevel) {
        public PotionEntry {
            if (minLevel < 0 || maxLevel < minLevel) throw new IllegalArgumentException("Invalid potion level range " + minLevel + ".." + maxLevel);
        }
    }

    /** Both pools of one sign; lists are copied on construction. */
    public record Entry(List<AffixEntry> enchantments, List<PotionEntry> potions) {
        public Entry {
            enchantments = List.copyOf(enchantments);
            potions = List.copyOf(potions);
        }
    }

    /** Code-registered defaults: permanent across reloads; duplicate ids fail fast. */
    private static final Map<ResourceLocation, Entry> CODE_ENTRIES = new LinkedHashMap<>();
    /** Datapack-driven entries: wholesale replaced after every successful data reload. */
    private static volatile Map<ResourceLocation, Entry> DATA_ENTRIES = Map.of();

    private SignImprintEffectRegistry() {}

    public static synchronized void register(ResourceLocation sign, List<AffixEntry> enchants, List<PotionEntry> potions) {
        if (CODE_ENTRIES.putIfAbsent(sign, new Entry(enchants, potions)) != null)
            throw new IllegalStateException("Duplicate imprint effects for " + sign);
    }

    public static void register(Sign sign, List<AffixEntry> enchants, List<PotionEntry> potions) {
        register(sign.id(), enchants, potions);
    }

    /** Replaces the datapack-driven portion atomically; code-registered defaults are untouched. */
    public static synchronized void replaceDataDriven(Map<ResourceLocation, Entry> entries) {
        DATA_ENTRIES = Map.copyOf(entries);
    }

    /** Datapack entries win over code-registered defaults (same precedence as domain traits). */
    public static List<AffixEntry> enchantments(ResourceLocation sign) {
        var entry = DATA_ENTRIES.get(sign);
        if (entry == null) entry = CODE_ENTRIES.get(sign);
        return entry == null ? List.of() : entry.enchantments();
    }

    public static List<PotionEntry> potions(ResourceLocation sign) {
        var entry = DATA_ENTRIES.get(sign);
        if (entry == null) entry = CODE_ENTRIES.get(sign);
        return entry == null ? List.of() : entry.potions();
    }

    /** Interpolated level: min + round((max - min) * proportion). */
    public static int levelFor(int minLevel, int maxLevel, double proportion) {
        return minLevel + (int) Math.round((maxLevel - minLevel) * proportion);
    }

    /**
     * Imprint conflict semantics, kept generic for pure JUnit coverage: iterate candidates in
     * order; skip the non-applicable; on the first conflict with anything already in the pool,
     * abandon every remaining candidate of this sign (labelled-continue equivalent).
     * The compatibility predicate sees the full candidate (so flags like ignoreCompatibility
     * stay visible) against unwrapped pool elements.
     */
    public static <C, E> List<C> selectApplicable(List<C> candidates, Predicate<C> applicable, BiPredicate<C, E> compatible,
                                                  Function<C, E> unwrap, Collection<E> existing) {
        var result = new ArrayList<C>();
        var pool = new ArrayList<E>(existing);
        for (C candidate : candidates) {
            if (!applicable.test(candidate)) continue;
            boolean conflict = false;
            for (E accepted : pool) if (!compatible.test(candidate, accepted)) { conflict = true; break; }
            if (conflict) break;
            result.add(candidate);
            pool.add(unwrap.apply(candidate));
        }
        return result;
    }
}
