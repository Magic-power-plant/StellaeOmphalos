package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BoonAttributeClampRegistry {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<ResourceLocation, BoonAttributeClamp> LIMITS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> WARNED_UNREGISTERED = ConcurrentHashMap.newKeySet();

    private BoonAttributeClampRegistry() {}

    public static void registerLimit(ResourceLocation attributeId, BoonAttributeClamp clamp) {
        if (LIMITS.putIfAbsent(attributeId, clamp) != null)
            throw new IllegalStateException("duplicate clamp for " + attributeId);
    }

    /** 登记处覆盖优先，否则回落到属性自带钳制；"未注册"与"未限值"统一为无界。 */
    public static BoonAttributeClamp clampFor(BoonAttribute attribute) {
        var limit = LIMITS.get(attribute.id());
        if (limit == null) return attribute.clamp();
        if (!BoonAttributeRegistry.isRegistered(attribute.id()) && WARNED_UNREGISTERED.add(attribute.id()))
            LOGGER.warn("Clamp registered for unregistered boon attribute {}", attribute.id());
        return limit;
    }
}
