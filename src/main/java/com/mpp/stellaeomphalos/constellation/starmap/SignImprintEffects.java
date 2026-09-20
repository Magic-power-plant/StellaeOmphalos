package com.mpp.stellaeomphalos.constellation.starmap;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.constellation.sign.SignBootstrap;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import com.mpp.stellaeomphalos.data.codec.FoundationCodecs;
import com.mpp.stellaeomphalos.data.loader.DataLoadReport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Data-table schema of {@code sign_imprint_effects}
 * (data/stellaeomphalos/sign_imprint_effects/&lt;sign&gt;.json; one sign per file, plan 2.2.7 / 2.4.5).
 * The file name binds the sign: the {@code sign} field must repeat it. Enchantment / mob-effect ids
 * and level ranges are validated at load; a single bad file rejects the whole table (framework
 * rollback), while sign existence can only be decided at rebuild time (the sign table is applied in
 * the same reload pass) and is therefore enforced when the registry is rebuilt.
 */
public final class SignImprintEffects {
    public record AffixSpec(ResourceLocation enchantment, int minLevel, int maxLevel, boolean ignoreCompatibility) {
        public AffixSpec {
            if (minLevel < 1 || maxLevel < minLevel)
                throw new IllegalArgumentException("Invalid affix level range " + minLevel + ".." + maxLevel);
        }
        public static final Codec<AffixSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("enchantment").forGetter(AffixSpec::enchantment),
                Codec.INT.fieldOf("min_level").forGetter(AffixSpec::minLevel),
                Codec.INT.fieldOf("max_level").forGetter(AffixSpec::maxLevel),
                FoundationCodecs.optional(Codec.BOOL, "ignore_compatibility", false).forGetter(AffixSpec::ignoreCompatibility)
        ).apply(instance, AffixSpec::new));
    }

    public record PotionSpec(ResourceLocation effect, int minLevel, int maxLevel) {
        public PotionSpec {
            if (minLevel < 0 || maxLevel < minLevel)
                throw new IllegalArgumentException("Invalid potion level range " + minLevel + ".." + maxLevel);
        }
        public static final Codec<PotionSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(PotionSpec::effect),
                Codec.INT.fieldOf("min_level").forGetter(PotionSpec::minLevel),
                Codec.INT.fieldOf("max_level").forGetter(PotionSpec::maxLevel)
        ).apply(instance, PotionSpec::new));
    }

    /** One file: {@code {"schema_version":1,"sign":"<namespace>:<file>","enchantments":[...],"potions":[...]}}. */
    public record Entry(int schemaVersion, ResourceLocation sign, List<AffixSpec> enchantments, List<PotionSpec> potions) {
        public Entry {
            enchantments = List.copyOf(enchantments);
            potions = List.copyOf(potions);
        }
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 1).fieldOf("schema_version").forGetter(Entry::schemaVersion),
                ResourceLocation.CODEC.fieldOf("sign").forGetter(Entry::sign),
                FoundationCodecs.optional(AffixSpec.CODEC.listOf(), "enchantments", List.<AffixSpec>of())
                        .forGetter(Entry::enchantments),
                FoundationCodecs.optional(PotionSpec.CODEC.listOf(), "potions", List.<PotionSpec>of())
                        .forGetter(Entry::potions)
        ).apply(instance, Entry::new));
    }

    private SignImprintEffects() {}

    /**
     * Table validator: cross-file checks and registry id existence. Two files declaring the same
     * sign are an entry-level conflict and reject the whole table; identical file paths across
     * datapacks never reach this point (the resource manager keeps only the highest-priority pack).
     * An unregistered sign only warns here — datapack signs become resolvable after the sign table
     * of the same reload is applied; the rebuild step drops any still-unknown sign.
     */
    public static void validate(Map<ResourceLocation, Entry> entries, DataLoadReport report) {
        var claimed = new HashMap<ResourceLocation, ResourceLocation>();
        entries.forEach((file, entry) -> {
            if (!entry.sign().equals(file))
                report.error(file.toString(), "$.sign", "Sign field must match the file name: " + entry.sign());
            var previous = claimed.putIfAbsent(entry.sign(), file);
            if (previous != null)
                report.error(file.toString(), "$.sign", "Sign already defined by " + previous);
            entry.enchantments().forEach(affix -> {
                if (BuiltInRegistries.ENCHANTMENT.getOptional(affix.enchantment()).isEmpty())
                    report.error(file.toString(), "$.enchantments", "Unknown enchantment " + affix.enchantment());
            });
            entry.potions().forEach(potion -> {
                if (BuiltInRegistries.MOB_EFFECT.getOptional(potion.effect()).isEmpty())
                    report.error(file.toString(), "$.potions", "Unknown mob effect " + potion.effect());
            });
            if (SignRegistry.byId(entry.sign()) == null && !SignBootstrap.BUILTIN_SIGN_IDS.contains(entry.sign()))
                report.warn(file.toString(), "$.sign", "Sign not registered yet: " + entry.sign());
        });
    }

    /**
     * Resolves a decoded entry to registry entries with live holders; null when an id is missing
     * (defensive — the validator already rejects unknown ids with a whole-table rollback).
     */
    public static @Nullable SignImprintEffectRegistry.Entry convert(Entry entry) {
        var enchants = new ArrayList<SignImprintEffectRegistry.AffixEntry>();
        for (var affix : entry.enchantments()) {
            var holder = BuiltInRegistries.ENCHANTMENT.getHolder(ResourceKey.create(Registries.ENCHANTMENT, affix.enchantment()));
            if (holder.isEmpty()) {
                LogUtils.getLogger().warn("Imprint effects for {} reference unknown enchantment {}", entry.sign(), affix.enchantment());
                return null;
            }
            enchants.add(new SignImprintEffectRegistry.AffixEntry(holder.get(), affix.minLevel(), affix.maxLevel(),
                    affix.ignoreCompatibility()));
        }
        var potions = new ArrayList<SignImprintEffectRegistry.PotionEntry>();
        for (var potion : entry.potions()) {
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceKey.create(Registries.MOB_EFFECT, potion.effect()));
            if (holder.isEmpty()) {
                LogUtils.getLogger().warn("Imprint effects for {} reference unknown mob effect {}", entry.sign(), potion.effect());
                return null;
            }
            potions.add(new SignImprintEffectRegistry.PotionEntry(holder.get(), potion.minLevel(), potion.maxLevel()));
        }
        return new SignImprintEffectRegistry.Entry(enchants, potions);
    }
}
