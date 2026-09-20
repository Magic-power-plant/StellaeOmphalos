package com.mpp.stellaeomphalos.ritual.rite;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public interface RitePedestal {
    enum OutputHoldMode {
        HELD,
        AUTO_INVENTORY,
        DROP_ON_FULL
    }

    UUID owner();

    void setOwner(UUID owner);

    OutputHoldMode holdMode();

    boolean tryDeliver(ServerPlayer player);
}
