package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;

/** Read-only domain capability; display formatting remains in the client. */
@FunctionalInterface
public interface BoonGauge {
    GaugeReading read(Player player);
}
