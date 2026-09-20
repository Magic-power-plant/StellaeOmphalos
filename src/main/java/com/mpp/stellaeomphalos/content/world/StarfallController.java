package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.network.OmphalosChannel;
import com.mpp.stellaeomphalos.network.toClient.StarfallNoticePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;

import java.util.*;

/** Ambient sky scheduling is separate from the damaging Part-2 meteor effect. */
public final class StarfallController {
    private static final Map<UUID, Long> DAYS = new HashMap<>();

    private StarfallController() {}

    public static void tick(ServerLevel level) {
        if (!level.dimensionType().hasSkyLight()
                || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return;
        long time = Math.floorMod(level.getDayTime(), 24000),
                day = Math.floorDiv(level.getDayTime(), 24000);
        if (time < 16000 || time > 20000) return;
        for (var player : level.players())
            if (DAYS.getOrDefault(player.getUUID(), Long.MIN_VALUE) != day
                    && player.getPersistentData().getLong("LastStarfallDay") != day + 1
                    && level.random.nextInt(3000) == 0) {
                DAYS.put(player.getUUID(), day);
                player.getPersistentData().putLong("LastStarfallDay", day + 1);
                forceSpawn(level, player.blockPosition(), player);
            }
    }

    public static void forceSpawn(ServerLevel level, BlockPos p, ServerPlayer player) {
        var starfall = WorldContent.STARFALL.get().create(level);
        int azimuth = level.random.nextInt(3600);
        if (starfall != null) {
            starfall.setPos(p.getX(), 560, p.getZ());
            double angle = Math.toRadians(azimuth / 10.0);
            starfall.setDeltaMovement(Math.sin(angle) * 0.2, 0, Math.cos(angle) * 0.2);
            level.addFreshEntity(starfall);
        }
        OmphalosChannel.send(
                player,
                new StarfallNoticePayload(
                        level.dimension().location(), p.getX(), p.getZ(), azimuth));
    }

    public static void logout(UUID player) {
        DAYS.remove(player);
    }

    public static void clear() {
        DAYS.clear();
    }
}
