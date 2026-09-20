package com.mpp.stellaeomphalos.constellation.boon;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.attribute.BoonAttributeRegistry;
import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import com.mpp.stellaeomphalos.data.loader.DataLoadReport;
import com.mpp.stellaeomphalos.data.loader.DataTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Boon tree data table declaration and tree assembly. The table is declared in the class-loading
 * window (contract); {@link #rebuildFromTables()} constructs a fresh, frozen {@link BoonTree}
 * instance and atomically swaps it in — the sanctioned reload path for a frozen tree.
 */
public final class BoonTreeTables {

    /**
     * Declared in the class-loading window per contract. When the module is attached late
     * (GameTest runs before the integrator wires the bootstrap into the mod constructor) the
     * registry is already frozen; the undeclared table is kept so the schema stays reachable,
     * and reload-driven rebuilds simply skip it.
     */
    public static final DataTable<BoonTreeDefinition.Definition> BOON_TREE = declare();

    private static DataTable<BoonTreeDefinition.Definition> declare() {
        var table = new DataTable<>(new ResourceLocation(Omphalos.MODID, "boon_tree"),
                BoonTreeDefinition.Definition.CODEC, BoonTreeTables::validate);
        try {
            return DataBootstrap.TABLES.declare(table);
        } catch (IllegalStateException lateDeclaration) {
            LogUtils.getLogger().warn("Boon tree table declared after the data window; datapack reloads will not rebuild the tree");
            return table;
        }
    }

    private BoonTreeTables() {}

    /** Cross-entry checks: duplicate node ids, global coordinate collisions, dangling edge endpoints. */
    static void validate(Map<ResourceLocation, BoonTreeDefinition.Definition> entries, DataLoadReport report) {
        var nodeIds = new HashSet<ResourceLocation>();
        var positions = new HashMap<Long, ResourceLocation>();
        entries.forEach((file, definition) -> {
            for (var node : definition.nodes()) {
                if (!nodeIds.add(node.id()))
                    report.error(file.toString(), "$.nodes", "Node id " + node.id() + " also defined by another tree file");
                var previous = positions.putIfAbsent(BoonTree.positionKey(node.x(), node.z()), node.id());
                if (previous != null && !previous.equals(node.id()))
                    report.error(file.toString(), "$.nodes", "Position " + node.x() + "," + node.z()
                            + " shared by " + node.id() + " and " + previous);
            }
        });
        entries.forEach((file, definition) -> definition.edges().forEach(edge -> {
            if (!nodeIds.contains(edge.getFirst()) || !nodeIds.contains(edge.getSecond()))
                report.error(file.toString(), "$.edges", "Edge references unknown node " + edge.getFirst() + " -> " + edge.getSecond());
        }));
    }

    /** Rebuilds the global tree from the current table snapshot; keeps the old tree on failure. */
    public static void rebuildFromTables() {
        var entries = DataBootstrap.TABLES.entries(BOON_TREE);
        try {
            var fresh = build(entries);
            fresh.freeze();
            BoonTree.rebuild(fresh);
            LogUtils.getLogger().info("Boon tree rebuilt: {} nodes, {} edges", fresh.nodes().size(), fresh.renderEdges().size());
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Boon tree rebuild failed; keeping the previous tree", exception);
        }
    }

    /** Builds a complete, still-unfrozen tree from table entries. Package-visible for tests. */
    public static BoonTree build(Map<ResourceLocation, BoonTreeDefinition.Definition> entries) {
        var tree = new BoonTree();
        var sorted = entries.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        for (var entry : sorted)
            for (var spec : entry.getValue().nodes()) registerNode(tree, entry.getValue(), spec);
        for (var entry : sorted)
            for (var edge : entry.getValue().edges()) {
                var a = tree.node(edge.getFirst());
                var b = tree.node(edge.getSecond());
                if (a == null || b == null)
                    throw new IllegalStateException("Edge references unknown node " + edge.getFirst() + " -> " + edge.getSecond());
                tree.connect(a, b);
            }
        return tree;
    }

    private static void registerNode(BoonTree tree, BoonTreeDefinition.Definition definition, BoonTreeDefinition.NodeSpec spec) {
        var node = createNode(definition, spec);
        if (node instanceof RootBoonNode root) tree.registerRoot(definition.sign().orElseThrow(), root);
        else tree.register(node);
    }

    private static BoonNode createNode(BoonTreeDefinition.Definition definition, BoonTreeDefinition.NodeSpec spec) {
        var modifiers = new ArrayList<BoonModifier>();
        for (var modifierSpec : spec.modifiers())
            modifiers.add(new BoonModifier(BoonAttributeRegistry.require(modifierSpec.attr()),
                    modifierSpec.mode(), modifierSpec.value(), false));
        var translators = new ArrayList<BoonTranslator>();
        for (var translatorSpec : spec.translators()) translators.add(createTranslator(translatorSpec));
        var extra = new CompoundTag();
        var requires = Set.copyOf(spec.rule().requires());
        int minLevel = spec.rule().minLevel();
        return switch (spec.type()) {
            case ROOT -> new RootBoonNode(spec.id(), spec.x(), spec.z(),
                    definition.sign().orElseThrow(() -> new IllegalArgumentException("Root node " + spec.id() + " needs a sign")),
                    spec.expMultiplier(), modifiers, translators, extra);
            case CORE_ROOT -> new CoreRootBoonNode(spec.id(), spec.x(), spec.z(), modifiers, translators, extra);
            case KEY -> minLevel > 0
                    ? new GatedBoonNode(spec.id(), spec.type(), spec.x(), spec.z(), minLevel, modifiers, translators, requires, extra)
                    : new KeyBoonNode(spec.id(), spec.x(), spec.z(), modifiers, translators, requires, extra);
            case MAJOR -> minLevel > 0
                    ? new GatedBoonNode(spec.id(), spec.type(), spec.x(), spec.z(), minLevel, modifiers, translators, requires, extra)
                    : new MajorBoonNode(spec.id(), spec.x(), spec.z(), modifiers, translators, requires, extra);
            case NORMAL -> minLevel > 0
                    ? new GatedBoonNode(spec.id(), spec.type(), spec.x(), spec.z(), minLevel, modifiers, translators, requires, extra)
                    : new BoonNode(spec.id(), BoonNodeType.NORMAL, spec.x(), spec.z(), modifiers, translators,
                            BoonUnlockRules.standard(requires), extra);
            case SOCKET -> new SocketBoonNode(spec.id(), spec.x(), spec.z(), modifiers, translators, requires, extra);
            case CONNECTOR -> new ConnectorBoonNode(spec.id(), spec.x(), spec.z(), modifiers, translators, extra);
        };
    }

    private static BoonTranslator createTranslator(BoonTreeDefinition.TranslatorSpec spec) {
        return switch (spec.type()) {
            case "identity" -> BoonTranslator.IDENTITY;
            case "radius_scale" -> {
                double factor = spec.factor();
                var scale = new BoonTranslator() {
                    @Override
                    public List<BoonModifier> translate(net.minecraft.world.entity.player.Player player, BoonModifier in,
                                                        ResourceLocation ownerNode) {
                        return List.of(in.withValue(in.value() * factor));
                    }
                };
                yield scale.withinRadius(spec.x(), spec.z(), spec.radius());
            }
            default -> throw new IllegalArgumentException("Unknown translator type " + spec.type());
        };
    }
}
