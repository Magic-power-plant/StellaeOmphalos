package com.mpp.stellaeomphalos.lumen.transport.stasis;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/** Serializable shell of a zone for views and sync; identity/equality is center + radius only. */
public record StasisZoneView(BlockPos center, double radius, StasisFilter.Mode filterMode, Optional<UUID> owner,
                             int particleTier, StasisZone.Phase phase, long remainingTicks) {
    public StasisZoneView {
        center = center.immutable();
        owner = Objects.requireNonNull(owner);
    }
    @Override public boolean equals(Object other) {
        return other instanceof StasisZoneView view && center.equals(view.center) && radius == view.radius;
    }
    @Override public int hashCode() { return Objects.hash(center, radius); }
}
