package com.mpp.stellaeomphalos.constellation.boon;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.data.codec.FoundationCodecs;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Data-table entry schema for one boon tree file (data/stellaeomphalos/boon_tree/&lt;sign&gt;.json).
 * Per-entry invariants (unique coordinates, exactly one root-like node, root implies sign) are
 * enforced at decode time; cross-entry checks live in the table validator.
 */
public final class BoonTreeDefinition {

    /** Edges are two-element arrays of node ids, e.g. ["a","b"]. */
    private static final Codec<Pair<ResourceLocation, ResourceLocation>> EDGE_CODEC =
            ResourceLocation.CODEC.listOf().flatXmap(list -> {
                if (list.size() != 2) return com.mojang.serialization.DataResult.error(() -> "An edge needs exactly 2 endpoints");
                return com.mojang.serialization.DataResult.success(Pair.of(list.get(0), list.get(1)));
            }, pair -> com.mojang.serialization.DataResult.success(List.of(pair.getFirst(), pair.getSecond())));

    public record ModifierSpec(ResourceLocation attr, BoonModifier.Mode mode, double value) {
        public static final Codec<BoonModifier.Mode> MODE_CODEC = Codec.STRING.xmap(
                name -> BoonModifier.Mode.fromOrdinal(modeOrdinal(name)),
                mode -> mode.name().toLowerCase(Locale.ROOT));

        private static int modeOrdinal(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "addition" -> 0;
                case "added_multiply" -> 1;
                case "stacking_multiply" -> 2;
                default -> throw new IllegalArgumentException("Unknown modifier mode " + name);
            };
        }

        public static final Codec<ModifierSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("attr").forGetter(ModifierSpec::attr),
                MODE_CODEC.fieldOf("mode").forGetter(ModifierSpec::mode),
                Codec.DOUBLE.fieldOf("value").forGetter(ModifierSpec::value)
        ).apply(instance, ModifierSpec::new));
    }

    /** Translator kinds: "identity" (no-op) and "radius_scale" (scale modifiers owned inside a grid radius). */
    public record TranslatorSpec(String type, int x, int z, double radius, double factor) {
        public static final Codec<TranslatorSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("type").forGetter(TranslatorSpec::type),
                FoundationCodecs.optional(Codec.INT, "x", 0).forGetter(TranslatorSpec::x),
                FoundationCodecs.optional(Codec.INT, "z", 0).forGetter(TranslatorSpec::z),
                FoundationCodecs.optional(Codec.DOUBLE, "radius", 0.0).forGetter(TranslatorSpec::radius),
                FoundationCodecs.optional(Codec.DOUBLE, "factor", 1.0).forGetter(TranslatorSpec::factor)
        ).apply(instance, TranslatorSpec::new));

        public TranslatorSpec {
            if (!type.equals("identity") && !type.equals("radius_scale"))
                throw new IllegalArgumentException("Unknown translator type " + type);
            if (type.equals("radius_scale") && (radius <= 0 || !Double.isFinite(factor) || factor <= 0))
                throw new IllegalArgumentException("radius_scale needs positive radius and factor");
        }
    }

    public record RuleSpec(List<ResourceLocation> requires, int minLevel) {
        public static final RuleSpec DEFAULT = new RuleSpec(List.of(), 0);

        public static final Codec<RuleSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FoundationCodecs.optional(ResourceLocation.CODEC.listOf(), "requires", List.<ResourceLocation>of())
                        .forGetter(RuleSpec::requires),
                FoundationCodecs.optional(Codec.INT, "min_level", 0).forGetter(RuleSpec::minLevel)
        ).apply(instance, RuleSpec::new));
    }

    public record NodeSpec(ResourceLocation id, BoonNodeType type, int x, int z,
                           List<ModifierSpec> modifiers, List<TranslatorSpec> translators,
                           RuleSpec rule, double expMultiplier) {
        public static final Codec<NodeSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(NodeSpec::id),
                BoonNodeType.CODEC.fieldOf("type").forGetter(NodeSpec::type),
                Codec.INT.fieldOf("x").forGetter(NodeSpec::x),
                Codec.INT.fieldOf("z").forGetter(NodeSpec::z),
                FoundationCodecs.optional(ModifierSpec.CODEC.listOf(), "modifiers", List.<ModifierSpec>of())
                        .forGetter(NodeSpec::modifiers),
                FoundationCodecs.optional(TranslatorSpec.CODEC.listOf(), "translators", List.<TranslatorSpec>of())
                        .forGetter(NodeSpec::translators),
                FoundationCodecs.optional(RuleSpec.CODEC, "rule", RuleSpec.DEFAULT).forGetter(NodeSpec::rule),
                FoundationCodecs.optional(Codec.DOUBLE, "exp_multiplier", 1.0).forGetter(NodeSpec::expMultiplier)
        ).apply(instance, NodeSpec::new));
    }

    public record Definition(int schemaVersion, Optional<ResourceLocation> sign, List<NodeSpec> nodes,
                             List<Pair<ResourceLocation, ResourceLocation>> edges) {
        public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 1).fieldOf("schema_version").forGetter(Definition::schemaVersion),
                ResourceLocation.CODEC.optionalFieldOf("sign").forGetter(Definition::sign),
                NodeSpec.CODEC.listOf().fieldOf("nodes").forGetter(Definition::nodes),
                FoundationCodecs.optional(EDGE_CODEC.listOf(), "edges",
                        List.<Pair<ResourceLocation, ResourceLocation>>of()).forGetter(Definition::edges)
        ).apply(instance, Definition::new));

        public Definition {
            nodes = List.copyOf(nodes);
            edges = List.copyOf(edges);
            if (nodes.isEmpty()) throw new IllegalArgumentException("A boon tree needs at least one node");
            var positions = new HashSet<Long>();
            var ids = new HashSet<ResourceLocation>();
            int roots = 0;
            for (var node : nodes) {
                if (!ids.add(node.id())) throw new IllegalArgumentException("Duplicate node id " + node.id());
                if (!positions.add(BoonTree.positionKey(node.x(), node.z())))
                    throw new IllegalArgumentException("Duplicate node position " + node.x() + "," + node.z());
                if (node.type().isRootLike()) roots++;
                if (node.type() == BoonNodeType.CORE_ROOT && !node.id().equals(BoonUnlockRules.CORE_ROOT_ID))
                    throw new IllegalArgumentException("The core root node id must be " + BoonUnlockRules.CORE_ROOT_ID);
            }
            if (roots != 1) throw new IllegalArgumentException("A boon tree needs exactly one root node, got " + roots);
            boolean hasRoot = nodes.stream().anyMatch(node -> node.type() == BoonNodeType.ROOT);
            if (hasRoot && sign.isEmpty()) throw new IllegalArgumentException("A root node requires the sign field");
            if (sign.isPresent() && !hasRoot)
                throw new IllegalArgumentException("The sign field is only valid for a sign-rooted tree");
            for (var edge : edges)
                if (edge.getFirst().equals(edge.getSecond()))
                    throw new IllegalArgumentException("Self connection at " + edge.getFirst());
        }
    }

    private BoonTreeDefinition() {}

    static Set<ResourceLocation> nodeIdsOf(Definition definition) {
        return definition.nodes().stream().map(NodeSpec::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
