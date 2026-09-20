package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.constellation.starmap.StarLine;
import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;
import com.mpp.stellaeomphalos.data.codec.FoundationCodecs;
import com.mpp.stellaeomphalos.data.loader.DataLoadReport;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Data-table entry schema for one sign definition file (data/stellaeomphalos/sign/&lt;id&gt;.json). */
public final class SignDefinitions {
    public enum Kind {
        MAJOR, RITUAL, TRAIT, ANOMALOUS;
        public static final Codec<Kind> CODEC = Codec.STRING.xmap(Kind::byName, kind -> kind.name().toLowerCase(Locale.ROOT));
        public static Kind byName(String name) {
            try { return valueOf(name.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unknown sign kind " + name); }
        }
    }

    private static final Codec<StarPoint> STAR_POINT = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, StarPoint.GRID - 1).fieldOf("x").forGetter(StarPoint::x),
            Codec.intRange(0, StarPoint.GRID - 1).fieldOf("y").forGetter(StarPoint::y)
    ).apply(instance, StarPoint::new));

    private static final Codec<StarLine> STAR_LINE = RecordCodecBuilder.create(instance -> instance.group(
            STAR_POINT.fieldOf("a").forGetter(StarLine::a),
            STAR_POINT.fieldOf("b").forGetter(StarLine::b)
    ).apply(instance, StarLine::new));

    /**
     * One sign definition. Invariants enforced at decode time: star coordinates are inside the
     * grid (codec range), line endpoints exist in the star set, no self-connections, no duplicate
     * undirected lines; anomalous signs must bind an omen.
     */
    public record Definition(int schemaVersion, Kind kind, int color, List<ResourceLocation> signatureItems,
                             List<StarPoint> stars, List<StarLine> lines,
                             Set<MoonPhase> moonPhases, Optional<CelestialOmen> omen) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 1).fieldOf("schema_version").forGetter(Definition::schemaVersion),
                Kind.CODEC.fieldOf("kind").forGetter(Definition::kind),
                Codec.INT.fieldOf("color").forGetter(Definition::color),
                FoundationCodecs.optional(ResourceLocation.CODEC.listOf(), "signature_items", List.<ResourceLocation>of())
                        .forGetter(Definition::signatureItems),
                STAR_POINT.listOf().fieldOf("stars").forGetter(Definition::stars),
                FoundationCodecs.optional(STAR_LINE.listOf(), "lines", List.<StarLine>of()).forGetter(Definition::lines),
                FoundationCodecs.optional(MoonPhase.CODEC.listOf(), "moon_phases", List.<MoonPhase>of())
                        .forGetter(definition -> definition.moonPhases().stream().sorted().toList()),
                CelestialOmen.CODEC.optionalFieldOf("omen").forGetter(Definition::omen)
        ).apply(instance, (version, kind, color, items, stars, lines, phases, omen) ->
                new Definition(version, kind, color, items, stars, lines, new HashSet<>(phases), omen)));

        public Definition {
            signatureItems = List.copyOf(signatureItems);
            stars = List.copyOf(stars);
            lines = List.copyOf(lines);
            moonPhases = Set.copyOf(moonPhases);
            if (stars.size() < 2) throw new IllegalArgumentException("A sign needs at least 2 stars");
            var uniqueStars = new HashSet<>(stars);
            if (uniqueStars.size() != stars.size()) throw new IllegalArgumentException("Duplicate star point");
            var uniqueLines = new HashSet<StarLine>();
            for (var line : lines) {
                if (line.a().equals(line.b())) throw new IllegalArgumentException("Self connection at " + line.a());
                if (!uniqueStars.contains(line.a()) || !uniqueStars.contains(line.b()))
                    throw new IllegalArgumentException("Line endpoint missing from star set: " + line);
                if (!uniqueLines.add(line)) throw new IllegalArgumentException("Duplicate line " + line);
            }
            if (kind == Kind.ANOMALOUS && omen.isEmpty()) throw new IllegalArgumentException("Anomalous sign needs an omen");
            if (kind != Kind.ANOMALOUS && omen.isPresent()) throw new IllegalArgumentException("Only anomalous signs bind an omen");
        }
    }

    private SignDefinitions() {}

    /** Table validator: cross-entry checks only; per-entry invariants live in the record itself. */
    public static void validate(Map<ResourceLocation, Definition> entries, DataLoadReport report) {
        var missing = new java.util.TreeSet<>(SignBootstrap.BUILTIN_SIGN_IDS);
        entries.keySet().forEach(missing::remove);
        missing.forEach(id -> report.warn(id.toString(), "$", "Built-in sign definition missing"));
    }
}
