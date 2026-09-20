package com.mpp.stellaeomphalos.constellation.boon;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * Global boon tree registry. {@link #freeze()} is irreversible: afterwards register/connect throw.
 * The sanctioned datapack-reload path is {@link #rebuild(BoonTree)}, which atomically swaps a
 * fully built, freshly frozen instance (mirroring the sign registry's rebuild pattern).
 * Duplicate grid positions fail fast with {@link IllegalStateException} (AC-2.15).
 */
public final class BoonTree {

    public static final int BOON_TREE_VERSION = 1;

    private static volatile BoonTree INSTANCE = new BoonTree();

    private final Map<ResourceLocation, BoonNode> byId = new LinkedHashMap<>();
    private final Map<ResourceLocation, BoonNode> roots = new LinkedHashMap<>();
    private final Map<Long, BoonNode> byPosition = new LinkedHashMap<>();
    private final BoonGraph graph = new BoonGraph();
    private boolean frozen;

    /** @return the frozen active tree; throws while the bootstrap tree is still mutable. */
    public static BoonTree get() {
        var tree = INSTANCE;
        if (!tree.frozen) throw new IllegalStateException("Boon tree is not frozen yet");
        return tree;
    }

    /** True once the active instance has been frozen. */
    public static boolean ready() {
        return INSTANCE.frozen;
    }

    /** Publishes a new tree. Legal at any time; the caller is expected to have frozen it. */
    public static synchronized void rebuild(BoonTree fresh) {
        if (fresh == null || !fresh.frozen) throw new IllegalArgumentException("Replacement tree must be frozen");
        INSTANCE = fresh;
    }

    public void register(BoonNode node) {
        if (frozen) throw new IllegalStateException("Boon tree is frozen");
        if (byId.putIfAbsent(node.id(), node) != null)
            throw new IllegalStateException("Duplicate boon node id " + node.id());
        long key = positionKey(node.gridX(), node.gridZ());
        var previous = byPosition.putIfAbsent(key, node);
        if (previous != null)
            throw new IllegalStateException("Duplicate boon node position " + node.gridX() + "," + node.gridZ()
                    + " for " + node.id() + " and " + previous.id());
    }

    /** Registers the root node bound to a sign (one per sign). */
    public void registerRoot(ResourceLocation signId, BoonNode root) {
        register(root);
        if (roots.putIfAbsent(signId, root) != null)
            throw new IllegalStateException("Duplicate boon root for sign " + signId);
    }

    public void connect(BoonNode a, BoonNode b) {
        if (frozen) throw new IllegalStateException("Boon tree is frozen");
        if (byId.get(a.id()) != a || byId.get(b.id()) != b)
            throw new IllegalArgumentException("Both nodes must be registered before connecting");
        graph.connect(a, b);
    }

    public void freeze() {
        frozen = true;
        byId.values().forEach(BoonNode::freeze);
    }

    public boolean frozen() {
        return frozen;
    }

    @Nullable
    public BoonNode node(ResourceLocation id) {
        return byId.get(id);
    }

    /** Root node bound to a sign id, or null when that sign has no tree. */
    @Nullable
    public BoonNode rootOf(ResourceLocation signId) {
        return roots.get(signId);
    }

    public Collection<ResourceLocation> rootSigns() {
        return List.copyOf(roots.keySet());
    }

    public List<BoonNode> nodes() {
        return List.copyOf(byId.values());
    }

    public List<BoonEdge> renderEdges() {
        return graph.renderEdges();
    }

    BoonGraph graph() {
        return graph;
    }

    static long positionKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
