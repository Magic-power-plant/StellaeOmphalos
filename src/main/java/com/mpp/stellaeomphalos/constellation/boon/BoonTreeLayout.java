package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.sign.Sign;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side layout store. The server sends node grid coordinates (declared at registration
 * time); the client only renders them and never derives prerequisites or unlock state itself.
 */
public final class BoonTreeLayout {

    private static final Map<ResourceLocation, Map<ResourceLocation, BoonNodeLayout>> BY_SIGN = new ConcurrentHashMap<>();

    private BoonTreeLayout() {}

    public static void apply(Sign sign, List<BoonNodeLayout> layout) {
        applyId(sign.id(), layout);
    }

    /** Raw-id variant for entries whose sign cannot be resolved client-side (e.g. the core tree). */
    public static void applyId(ResourceLocation signId, List<BoonNodeLayout> layout) {
        var map = new ConcurrentHashMap<ResourceLocation, BoonNodeLayout>();
        layout.forEach(entry -> map.put(entry.id(), entry));
        BY_SIGN.put(signId, map);
    }

    public static Optional<BoonNodeLayout> layoutOf(ResourceLocation nodeId) {
        for (var map : BY_SIGN.values()) {
            var entry = map.get(nodeId);
            if (entry != null) return Optional.of(entry);
        }
        return Optional.empty();
    }

    public static Map<ResourceLocation, BoonNodeLayout> ofSign(ResourceLocation signId) {
        return Map.copyOf(BY_SIGN.getOrDefault(signId, Map.of()));
    }

    public static void clear() {
        BY_SIGN.clear();
    }

    /**
     * Coordinate contract with the codex star map (Part-5 owns codex regions): boon grid
     * coordinates map 1:1 onto codex space. Kept as one conversion point so a future scale
     * change touches a single method.
     */
    public static double[] gridToCodexSpace(int gridX, int gridZ) {
        return new double[] { gridX, gridZ };
    }

    /** Server-side helper: layout entries of the whole tree, for the sync payload. */
    public static List<BoonNodeLayout> snapshot(BoonTree tree) {
        return tree.nodes().stream()
                .map(node -> new BoonNodeLayout(node.id(), node.gridX(), node.gridZ(), (byte) node.type().ordinal()))
                .toList();
    }

    /** Groups a node id under its tree file's sign key (first path segment), e.g. "aevitas/root" → "aevitas". */
    @Nullable
    public static ResourceLocation signKeyOf(ResourceLocation nodeId) {
        var path = nodeId.getPath();
        int slash = path.indexOf('/');
        return slash < 0 ? null : new ResourceLocation(nodeId.getNamespace(), path.substring(0, slash));
    }
}
