package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Default unlock-rule implementations. Common elements: sealed nodes never unlock and never
 * count; progress-gated nodes are fully hidden below their threshold (no tooltip caching either,
 * since visibility is evaluated on demand).
 */
public final class BoonUnlockRules {

    /** Id of the single core root node; any root node requires it to be unlocked first. */
    public static final ResourceLocation CORE_ROOT_ID = new ResourceLocation(Omphalos.MODID, "core/root");

    private BoonUnlockRules() {}

    /** Standard rule: free point + at least one adjacent unlocked + explicit prerequisites. */
    public static UnlockRule standard(Set<ResourceLocation> requires) {
        var required = Set.copyOf(requires);
        return new UnlockRule() {
            @Override
            public boolean mayUnlock(net.minecraft.server.level.ServerPlayer player, BoonNode node, BoonProgressView progress) {
                if (!visible(player, progress) || progress.isSealed(node.id())) return false;
                if (progress.availablePoints() <= 0) return false;
                boolean adjacent = node.neighbors().stream().anyMatch(progress::hasNode);
                if (!adjacent) return false;
                return required.stream().allMatch(progress::hasNode);
            }

            @Override
            public boolean visible(net.minecraft.server.level.ServerPlayer player, BoonProgressView progress) {
                return true;
            }
        };
    }

    /** Root rule: the sign must be discovered and the core root unlocked; no adjacency needed. */
    public static UnlockRule root(ResourceLocation signId) {
        return new UnlockRule() {
            @Override
            public boolean mayUnlock(net.minecraft.server.level.ServerPlayer player, BoonNode node, BoonProgressView progress) {
                return visible(player, progress) && !progress.isSealed(node.id())
                        && progress.availablePoints() > 0 && progress.hasNode(CORE_ROOT_ID);
            }

            @Override
            public boolean visible(net.minecraft.server.level.ServerPlayer player, BoonProgressView progress) {
                return progress.knowsSign(signId);
            }
        };
    }

    /** Core root rule: every major sign must be discovered. */
    public static UnlockRule coreRoot() {
        return new UnlockRule() {
            @Override
            public boolean mayUnlock(net.minecraft.server.level.ServerPlayer player, BoonNode node, BoonProgressView progress) {
                if (progress.isSealed(node.id()) || progress.availablePoints() <= 0) return false;
                return SignRegistry.majorSigns().stream().allMatch(sign -> progress.knowsSign(sign.id()));
            }

            @Override
            public boolean visible(net.minecraft.server.level.ServerPlayer player, BoonProgressView progress) {
                return true;
            }
        };
    }

    /** Connector rule: every neighbor unlocked, or any other connector already unlocked. */
    public static UnlockRule connector() {
        return new UnlockRule() {
            @Override
            public boolean mayUnlock(net.minecraft.server.level.ServerPlayer player, BoonNode node, BoonProgressView progress) {
                if (progress.isSealed(node.id()) || progress.availablePoints() <= 0) return false;
                var neighbors = node.neighbors();
                if (!neighbors.isEmpty() && neighbors.stream().allMatch(progress::hasNode)) return true;
                return BoonTree.ready() && BoonTree.get().nodes().stream()
                        .anyMatch(other -> other.type() == BoonNodeType.CONNECTOR && progress.hasNode(other.id()));
            }

            @Override
            public boolean visible(net.minecraft.server.level.ServerPlayer player, BoonProgressView progress) {
                return true;
            }
        };
    }

    /** Progress gate: below the level threshold the node is completely hidden. */
    public static UnlockRule gated(int minLevel, UnlockRule delegate) {
        if (minLevel < 1) throw new IllegalArgumentException("Nonpositive gate level");
        return new UnlockRule() {
            @Override
            public boolean mayUnlock(net.minecraft.server.level.ServerPlayer player, BoonNode node, BoonProgressView progress) {
                return visible(player, progress) && delegate.mayUnlock(player, node, progress);
            }

            @Override
            public boolean visible(net.minecraft.server.level.ServerPlayer player, BoonProgressView progress) {
                return progress.level() >= minLevel && delegate.visible(player, progress);
            }
        };
    }
}
