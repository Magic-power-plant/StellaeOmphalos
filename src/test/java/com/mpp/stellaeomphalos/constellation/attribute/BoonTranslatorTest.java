package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonTranslatorTest {

    private static final ResourceLocation NODE_A = new ResourceLocation("stellaeomphalos", "test_node_a");
    private static final ResourceLocation NODE_B = new ResourceLocation("stellaeomphalos", "test_node_b");
    private static final ResourceLocation ATTR_ID = new ResourceLocation("stellaeomphalos", "test_translator_attr");

    private static final BoonAttribute attribute =
            BoonAttributeRegistry.register(new BoonAttribute(ATTR_ID, 0.0, BoonAttributeClamp.UNBOUNDED, false));

    @AfterEach
    void clearLookup() {
        BoonTranslator.setNodeGridLookup(null);
    }

    private BoonModifier modifier(double value) {
        return new BoonModifier(attribute, BoonModifier.Mode.ADDITION, value, false).bind(NODE_A, 0);
    }

    private static BoonTranslator doubler() {
        return new BoonTranslator() {
            @Override
            public List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode) {
                return List.of(in.withValue(in.value() * 2.0));
            }
        };
    }

    @Test void identityPassesThrough() {
        var modifier = modifier(3.0);
        assertEquals(List.of(modifier), BoonTranslator.IDENTITY.translate(null, modifier, NODE_A));
    }

    @Test void andThenChainsSequentially() {
        var chained = doubler().andThen(doubler());
        var out = chained.translate(null, modifier(3.0), NODE_A);
        assertEquals(1, out.size());
        assertEquals(12.0, out.get(0).value());
    }

    @Test void andThenConcatenatesExtraModifiers() {
        var extra = new BoonTranslator() {
            @Override
            public List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode) {
                return List.of(in);
            }

            @Override
            public List<BoonModifier> extraModifiers(Player player, BoonModifier in) {
                return List.of(in.withValue(7.0));
            }
        };
        var chained = BoonTranslator.IDENTITY.andThen(extra);
        assertEquals(List.of(), BoonTranslator.IDENTITY.extraModifiers(null, modifier(1.0)));
        assertEquals(1, chained.extraModifiers(null, modifier(1.0)).size());
        assertEquals(7.0, chained.extraModifiers(null, modifier(1.0)).get(0).value());
    }

    @Test void withinRadiusAppliesOnlyInsideEuclideanGridDistance() {
        BoonTranslator.setNodeGridLookup(nodeId -> {
            if (nodeId.equals(NODE_A)) return new int[]{0, 0};
            if (nodeId.equals(NODE_B)) return new int[]{10, 10};
            return null;
        });
        var wrapped = doubler().withinRadius(0, 0, 5.0);
        assertEquals(6.0, wrapped.translate(null, modifier(3.0), NODE_A).get(0).value());
        assertEquals(3.0, wrapped.translate(null, modifier(3.0), NODE_B).get(0).value());
        // 边界：距离恰为半径算在内（3-4-5 直角）
        BoonTranslator.setNodeGridLookup(nodeId -> new int[]{3, 4});
        assertEquals(6.0, wrapped.translate(null, modifier(3.0), NODE_A).get(0).value());
    }

    @Test void withinRadiusSkipsExtrasOutsideAndPassesThroughWithoutLookup() {
        BoonTranslator.setNodeGridLookup(nodeId -> new int[]{100, 100});
        var extra = new BoonTranslator() {
            @Override
            public List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode) {
                return List.of(in.withValue(0.0));
            }

            @Override
            public List<BoonModifier> extraModifiers(Player player, BoonModifier in) {
                return List.of(in.withValue(1.0));
            }
        };
        var wrapped = extra.withinRadius(0, 0, 5.0);
        assertEquals(3.0, wrapped.translate(null, modifier(3.0), NODE_A).get(0).value());
        assertEquals(List.of(), wrapped.extraModifiers(null, modifier(3.0)));
        // 未注入坐标查询钩子：保守放行原修饰符
        BoonTranslator.setNodeGridLookup(null);
        assertEquals(3.0, wrapped.translate(null, modifier(3.0), NODE_A).get(0).value());
    }

    @Test void absoluteModifiersSkipAllTranslation() {
        var normal = modifier(3.0);
        var absolute = new GemAffixModifier(java.util.UUID.randomUUID(), attribute, BoonModifier.Mode.ADDITION, 3.0)
                .bind(NODE_A, 1);
        var out = BoonValueBridge.applyTranslators(null, List.of(normal, absolute), List.of(doubler()));
        assertEquals(6.0, out.get(0).value());
        assertSame(absolute, out.get(1));
    }
}
