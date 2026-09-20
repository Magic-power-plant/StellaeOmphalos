package com.mpp.stellaeomphalos.constellation.domain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.data.codec.FoundationCodecs;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Query facade over the {@code domain_traits} data table (data/stellaeomphalos/domain_traits.json).
 * Every trait-sign modifier is a multiplicative scale over {@link DomainProperties}; neutral
 * defaults keep the record usable before the first successful data load. The four built-in rows
 * mirror plan 2.2.6.1 and act as the fallback when the table is absent or failed to load.
 */
public final class DomainTraits {
    /** Multiplicative scales; 1.0 / false are the neutral elements of {@link DomainProperties#modify}. */
    public record Modifier(double potencyScale, double sizeScale, double amplifierScale,
                           boolean corrupted, double fractureLowerScale, double fractureRateScale) {
        public static final Modifier NEUTRAL = new Modifier(1.0, 1.0, 1.0, false, 1.0, 1.0);
        public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FoundationCodecs.optional(Codec.DOUBLE, "potency_scale", 1.0).forGetter(Modifier::potencyScale),
                FoundationCodecs.optional(Codec.DOUBLE, "size_scale", 1.0).forGetter(Modifier::sizeScale),
                FoundationCodecs.optional(Codec.DOUBLE, "amplifier_scale", 1.0).forGetter(Modifier::amplifierScale),
                FoundationCodecs.optional(Codec.BOOL, "corrupted", false).forGetter(Modifier::corrupted),
                FoundationCodecs.optional(Codec.DOUBLE, "fracture_lower_scale", 1.0).forGetter(Modifier::fractureLowerScale),
                FoundationCodecs.optional(Codec.DOUBLE, "fracture_rate_scale", 1.0).forGetter(Modifier::fractureRateScale)
        ).apply(instance, Modifier::new));
    }

    /** Whole-file table shape: {@code {"schema_version":1,"traits":{"gelu":{...},...}}}. */
    public record TraitTable(Map<String, Modifier> traits) {
        public TraitTable { traits = Map.copyOf(traits); }
        public static final Codec<TraitTable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FoundationCodecs.optional(Codec.intRange(1, 1), "schema_version", 1).forGetter(table -> 1),
                Codec.unboundedMap(Codec.STRING, Modifier.CODEC).fieldOf("traits").forGetter(TraitTable::traits)
        ).apply(instance, (version, traits) -> new TraitTable(new LinkedHashMap<>(traits))));
    }

    private static final Map<ResourceLocation, Modifier> BUILTIN = Map.of(
            id("gelu"), new Modifier(0.15, 3.5, 1.0, false, 1.0, 1.0),
            id("ulteria"), new Modifier(1.0, 0.2, 4.0, false, 1.0, 1.0),
            id("alcara"), new Modifier(1.0, 2.0, 2.0, true, 0.015, 50000.0),
            id("vorux"), new Modifier(1.0, 1.75, 2.0, false, 0.25, 3000.0));

    private DomainTraits() {}

    private static ResourceLocation id(String path) { return new ResourceLocation(Omphalos.MODID, path); }

    /** Datapack rows win over the built-in defaults; unknown traits are neutral. */
    public static Modifier modifier(ResourceLocation traitId) {
        for (var table : DataBootstrap.TABLES.entries(DomainBootstrap.DOMAIN_TRAITS).values()) {
            var row = table.traits().get(traitId.getPath());
            if (row != null) return row;
        }
        return Optional.ofNullable(BUILTIN.get(traitId)).orElse(Modifier.NEUTRAL);
    }

    /** Built-in defaults, exposed for tests and documentation parity. */
    public static Map<ResourceLocation, Modifier> builtin() { return BUILTIN; }
}
