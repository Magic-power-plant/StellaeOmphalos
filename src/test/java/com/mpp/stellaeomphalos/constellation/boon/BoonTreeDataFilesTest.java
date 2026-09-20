package com.mpp.stellaeomphalos.constellation.boon;

import com.mojang.serialization.JsonOps;
import com.mpp.stellaeomphalos.constellation.attribute.BoonAttribute;
import com.mpp.stellaeomphalos.constellation.attribute.BoonAttributeClamp;
import com.mpp.stellaeomphalos.constellation.attribute.BoonAttributeRegistry;
import com.mpp.stellaeomphalos.constellation.attribute.BoonAttributes;
import com.mpp.stellaeomphalos.data.codec.BoundedJson;
import com.mpp.stellaeomphalos.data.loader.DataLoadReport;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Decodes the shipped boon tree datapack JSON with the real codecs, catching schema drift. */
class BoonTreeDataFilesTest {

    private static final List<String> TREES = List.of("aevitas", "armara", "discidia", "evorsio", "vicio", "core");

    private static BoonTreeDefinition.Definition decode(String name) {
        var path = "/data/stellaeomphalos/boon_tree/" + name + ".json";
        try (var stream = BoonTreeDataFilesTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing classpath resource " + path);
            var json = BoundedJson.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return BoonTreeDefinition.Definition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, message -> {
                throw new AssertionError(path + ": " + message);
            });
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    /** The 8 vanilla-bridged attributes register with AttributeBootstrap; unit tests register placeholders. */
    private static void ensureAttributes() {
        BoonAttributes.init();
        for (String path : List.of("armor", "armor_toughness", "attack_speed", "max_health", "reach",
                "melee_damage", "movement_speed", "swim_speed")) {
            var id = new ResourceLocation("stellaeomphalos", path);
            if (!BoonAttributeRegistry.isRegistered(id))
                BoonAttributeRegistry.register(new BoonAttribute(id, 0.0, BoonAttributeClamp.UNBOUNDED, false));
        }
    }

    @Test
    void allSixTreesDecodeAndValidate() {
        var entries = new LinkedHashMap<ResourceLocation, BoonTreeDefinition.Definition>();
        for (var name : TREES)
            entries.put(new ResourceLocation("stellaeomphalos", name), decode(name));
        assertEquals(6, entries.size());
        entries.forEach((file, definition) -> {
            assertTrue(definition.nodes().size() >= 3 && definition.nodes().size() <= 7,
                    file + " node count " + definition.nodes().size());
            definition.nodes().forEach(node -> assertEquals("stellaeomphalos", node.id().getNamespace()));
            if (file.getPath().equals("core")) assertTrue(definition.sign().isEmpty(), "Core tree binds no sign");
            else assertEquals(new ResourceLocation("stellaeomphalos", file.getPath()), definition.sign().orElseThrow());
        });
        var report = new DataLoadReport();
        BoonTreeTables.validate(entries, report);
        assertTrue(report.issues().isEmpty(), report.issues().toString());
    }

    @Test
    void treeAssemblesWithRootsSocketsAndConnectors() {
        ensureAttributes();
        var entries = new LinkedHashMap<ResourceLocation, BoonTreeDefinition.Definition>();
        for (var name : TREES)
            entries.put(new ResourceLocation("stellaeomphalos", name), decode(name));
        var tree = BoonTreeTables.build(entries);
        tree.freeze();
        assertEquals(entries.values().stream().mapToInt(def -> def.nodes().size()).sum(), tree.nodes().size());
        var coreRoot = tree.node(BoonUnlockRules.CORE_ROOT_ID);
        assertInstanceOf(CoreRootBoonNode.class, coreRoot);
        for (String sign : List.of("aevitas", "armara", "discidia", "evorsio", "vicio")) {
            var root = tree.rootOf(new ResourceLocation("stellaeomphalos", sign));
            assertInstanceOf(RootBoonNode.class, root, sign + " root missing");
            assertFalse(root.modifiers().stream().anyMatch(m -> m.ownerNode() == null),
                    "Modifiers must bind to their owner node");
        }
        assertTrue(tree.nodes().stream().anyMatch(SocketBoonNode.class::isInstance), "At least one socket node ships");
        assertTrue(tree.nodes().stream().anyMatch(ConnectorBoonNode.class::isInstance), "At least one connector ships");
        assertTrue(tree.nodes().stream().anyMatch(GatedBoonNode.class::isInstance), "At least one gated node ships");
        // Symmetric adjacency: every render edge touches two nodes that list each other.
        for (var edge : tree.renderEdges()) {
            var a = tree.node(edge.a());
            var b = tree.node(edge.b());
            assertNotNull(a);
            assertNotNull(b);
            assertTrue(a.neighbors().contains(b.id()) && b.neighbors().contains(a.id()), "Adjacency must be symmetric");
        }
    }
}
