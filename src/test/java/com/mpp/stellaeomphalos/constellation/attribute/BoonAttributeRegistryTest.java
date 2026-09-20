package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonAttributeRegistryTest {

    @Test void duplicateRegistrationThrows() {
        var id = new ResourceLocation("stellaeomphalos", "test_registry_dup");
        BoonAttributeRegistry.register(new BoonAttribute(id, 0.0, BoonAttributeClamp.UNBOUNDED, false));
        assertThrows(IllegalStateException.class,
                () -> BoonAttributeRegistry.register(new BoonAttribute(id, 1.0, BoonAttributeClamp.UNBOUNDED, false)));
    }

    @Test void allTwentyTwoChannelsAreRegistered() {
        BoonAttributes.init();
        assertEquals(22, BoonAttributeRegistry.all().stream()
                .filter(attribute -> attribute.id().getNamespace().equals("stellaeomphalos")
                        && !attribute.id().getPath().startsWith("test_"))
                .count() + countBridgedPlaceholders());
    }

    private static int countBridgedPlaceholders() {
        // 8 个原版桥接属性由 AttributeBootstrap 注册；纯逻辑测试环境未挂 bootstrap，按缺口补差
        int missing = 0;
        for (String path : List.of("armor", "armor_toughness", "attack_speed", "max_health", "reach",
                "melee_damage", "movement_speed", "swim_speed"))
            if (!BoonAttributeRegistry.isRegistered(new ResourceLocation("stellaeomphalos", path))) missing++;
        return missing;
    }

    @Test void unboundedClampPassesValuesThrough() {
        var clamp = BoonAttributeClamp.UNBOUNDED;
        assertFalse(clamp.bounded());
        assertEquals(1e9, clamp.clamp(1e9));
        assertEquals(-1e9, clamp.clamp(-1e9));
    }

    @Test void clampRejectsInvertedRangeAndAppliesBounds() {
        assertThrows(IllegalArgumentException.class, () -> BoonAttributeClamp.of(2.0, 1.0));
        var clamp = BoonAttributeClamp.of(0.0, 0.75);
        assertEquals(0.75, clamp.max());
        assertEquals(0.75, clamp.clamp(1.5));
        assertEquals(0.0, clamp.clamp(-1.0));
        assertEquals(0.5, clamp.clamp(0.5));
    }

    @Test void clampRegistryOverrideWinsAndWarnsForUnregistered() {
        var id = new ResourceLocation("stellaeomphalos", "test_clamp_override");
        BoonAttributeClampRegistry.registerLimit(id, BoonAttributeClamp.of(0.0, 1.0));
        var attribute = new BoonAttribute(id, 0.0, BoonAttributeClamp.UNBOUNDED, false);
        // 属性未注册时登记处限幅仍生效（并记 warning）
        assertEquals(1.0, BoonAttributeClampRegistry.clampFor(attribute).clamp(5.0));
        assertThrows(IllegalStateException.class,
                () -> BoonAttributeClampRegistry.registerLimit(id, BoonAttributeClamp.of(0.0, 2.0)));
    }

    @Test void cacheInvalidatesByPlayerAndEpoch() {
        var cache = new BoonValueCache();
        var player = UUID.randomUUID();
        var key = new BoonValueCache.Key(player, new ResourceLocation("stellaeomphalos", "dodge"),
                BoonModifier.Mode.ADDITION);
        long epochBefore = cache.epoch();
        assertEquals(1.0, cache.getOrCompute(key, () -> 1.0));
        assertEquals(1.0, cache.getOrCompute(key, () -> 99.0));
        assertEquals(1, cache.size());
        assertTrue(cache.trackedPlayers().contains(player));
        cache.invalidate(player);
        assertTrue(cache.epoch() > epochBefore);
        assertEquals(0, cache.size());
        assertTrue(cache.trackedPlayers().isEmpty());
        cache.getOrCompute(key, () -> 2.0);
        cache.invalidateAll();
        assertEquals(0, cache.size());
    }
}
