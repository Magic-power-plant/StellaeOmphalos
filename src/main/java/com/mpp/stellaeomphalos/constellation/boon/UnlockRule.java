package com.mpp.stellaeomphalos.constellation.boon;

import net.minecraft.server.level.ServerPlayer;

/** Unlock and visibility rules for a boon node. Implementations must be side-effect free. */
public interface UnlockRule {
    boolean mayUnlock(ServerPlayer player, BoonNode node, BoonProgressView progress);

    boolean visible(ServerPlayer player, BoonProgressView progress);
}
