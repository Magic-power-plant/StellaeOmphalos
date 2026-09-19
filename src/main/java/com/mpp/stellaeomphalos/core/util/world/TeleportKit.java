package com.mpp.stellaeomphalos.core.util.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class TeleportKit {
    private TeleportKit() {}
    public static void withoutPortal(ServerPlayer player, ServerLevel destination, Vec3 position, float yaw, float pitch) {
        if (!destination.getServer().isSameThread()) throw new IllegalStateException("Teleport off server thread");
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) throw new IllegalArgumentException("Invalid destination");
        player.teleportTo(destination, position.x, position.y, position.z, yaw, pitch);
    }
}
