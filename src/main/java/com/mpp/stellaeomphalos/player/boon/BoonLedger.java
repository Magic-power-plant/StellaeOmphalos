package com.mpp.stellaeomphalos.player.boon;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Staging scheduler for batched applied-set changes: removals always run before additions
 * ("先拆后装"), so a rebuilt state never briefly double-counts. Scheduling the same id in both
 * sets cancels back to the latest intent.
 */
public final class BoonLedger {

    private final Set<ResourceLocation> pendingAdditions = new LinkedHashSet<>();
    private final Set<ResourceLocation> pendingRemovals = new LinkedHashSet<>();

    public void scheduleAdd(ResourceLocation nodeId) {
        pendingRemovals.remove(nodeId);
        pendingAdditions.add(nodeId);
    }

    public void scheduleRemove(ResourceLocation nodeId) {
        pendingAdditions.remove(nodeId);
        pendingRemovals.add(nodeId);
    }

    public boolean isEmpty() {
        return pendingAdditions.isEmpty() && pendingRemovals.isEmpty();
    }

    /** Applies removals first, then additions, against the applied set exposed by the view writer. */
    public void commit(Writer writer) {
        pendingRemovals.forEach(writer::remove);
        pendingAdditions.forEach(writer::add);
        pendingRemovals.clear();
        pendingAdditions.clear();
    }

    /** Write endpoint for a commit; implemented by the progress store. */
    public interface Writer {
        void add(ResourceLocation nodeId);

        void remove(ResourceLocation nodeId);
    }

    /** Derives the point cost of a set of applied ids under the root-exemption rule. */
    public static int spentPoints(java.util.function.Function<ResourceLocation, Boolean> rootLike,
                                  Set<ResourceLocation> applied, Set<ResourceLocation> sealed) {
        int spent = 0;
        for (var id : applied) {
            if (sealed.contains(id)) continue;
            if (Boolean.TRUE.equals(rootLike.apply(id))) continue;
            spent++;
        }
        return spent;
    }
}
