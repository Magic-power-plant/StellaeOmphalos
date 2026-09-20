package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GemAffixModifierTest {

    private static final BoonAttribute ATTR = BoonAttributeRegistry.register(new BoonAttribute(
            new ResourceLocation("stellaeomphalos", "test_gem_attr"), 0.0, BoonAttributeClamp.UNBOUNDED, false));

    @BeforeEach
    void clearCache() {
        GemAffixModifier.clearTranslationCache();
    }

    @Test void nbtRoundTripsWithPascalCaseKeys() {
        var gem = new GemAffixModifier(UUID.randomUUID(), ATTR, BoonModifier.Mode.STACKING_MULTIPLY, 1.25);
        var tag = gem.save();
        assertTrue(tag.contains("Gem") && tag.contains("Attr") && tag.contains("Mode") && tag.contains("Value"));
        assertEquals("stellaeomphalos:test_gem_attr", tag.getString("Attr"));
        var loaded = GemAffixModifier.load(tag);
        assertEquals(gem.gemId(), loaded.gemId());
        assertSame(ATTR, loaded.attribute());
        assertEquals(BoonModifier.Mode.STACKING_MULTIPLY, loaded.mode());
        assertEquals(1.25, loaded.value(), 1e-6);
    }

    @Test void gemsAreAlwaysAbsolute() {
        var gem = new GemAffixModifier(UUID.randomUUID(), ATTR, BoonModifier.Mode.ADDITION, 2.0);
        assertTrue(gem.absolute());
    }

    @Test void translationCacheIsBoundedByLru() {
        var doubler = new BoonTranslator() {
            @Override
            public List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode) {
                return List.of(in.withValue(in.value() * 2.0));
            }
        };
        for (int i = 0; i < 5000; i++) {
            var gem = new GemAffixModifier(UUID.randomUUID(), ATTR, BoonModifier.Mode.ADDITION, 1.0);
            var out = gem.translateCached(null, List.of(doubler));
            assertEquals(2.0, out.get(0).value());
        }
        assertTrue(GemAffixModifier.translationCacheSize() <= 4096,
                "cache size " + GemAffixModifier.translationCacheSize());
    }

    @Test void translationCacheHitsOnRepeatChain() {
        var gem = new GemAffixModifier(UUID.randomUUID(), ATTR, BoonModifier.Mode.ADDITION, 1.0);
        var first = gem.translateCached(null, List.of(BoonTranslator.IDENTITY));
        var second = gem.translateCached(null, List.of(BoonTranslator.IDENTITY));
        assertSame(first, second);
        assertEquals(1, GemAffixModifier.translationCacheSize());
    }
}
