package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.network.SafeDispatch;
import com.mpp.stellaeomphalos.network.toClient.PktGatewayTargets;
import com.mpp.stellaeomphalos.network.toServer.PktGatewayTravel;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Server-owned charge timeline; a session can be consumed once and never accepts arbitrary targets.
 */
@Mod.EventBusSubscriber(modid = Omphalos.MODID)
public final class GateNetworkService {
    private static final class Journey {
        final ResourceKey<Level> dimension;
        final BlockPos origin;
        final long token, expires;
        final Set<BlockPos> offered;
        BlockPos destination;
        long readyAt;

        Journey(ServerPlayer player, BlockPos origin, List<BlockPos> offered) {
            this.dimension = player.level().dimension();
            this.origin = origin.immutable();
            this.offered = Set.copyOf(offered);
            token = player.getRandom().nextLong();
            expires = player.server.getTickCount() + 1200;
        }
    }

    private static final Map<UUID, Journey> SESSIONS = new HashMap<>();

    private GateNetworkService() {}

    public static void open(ServerPlayer player, BlockPos origin) {
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !validOrigin(player, origin)) return;
        var targets =
                GateLedger.get(player.serverLevel()).destinations(player.serverLevel(), origin);
        var journey = new Journey(player, origin, targets);
        SESSIONS.put(player.getUUID(), journey);
        SafeDispatch.send(
                player,
                new PktGatewayTargets(
                        player.level().dimension().location(), origin, journey.token, targets));
    }

    private static boolean validOrigin(ServerPlayer p, BlockPos origin) {
        return p.isAlive()
                && p.serverLevel().hasChunkAt(origin)
                && p.distanceToSqr(origin.getX() + .5, origin.getY() + .5, origin.getZ() + .5) <= 36
                && p.serverLevel()
                        .getBlockState(origin)
                        .is(WorldContent.BLOCKS.get("gate_core").get())
                && p.serverLevel().mayInteract(p, origin);
    }

    public static boolean request(ServerPlayer player, PktGatewayTravel packet) {
        var journey = SESSIONS.get(player.getUUID());
        if (journey == null || journey.token != packet.token()) return false;
        if (packet.cancel()) {
            SESSIONS.remove(player.getUUID());
            return true;
        }
        if (journey.destination != null
                || player.server.getTickCount() > journey.expires
                || !player.level().dimension().equals(journey.dimension)
                || !validOrigin(player, journey.origin)
                || !journey.offered.contains(packet.target())) return false;
        journey.destination = packet.target().immutable();
        journey.readyAt = player.server.getTickCount() + 95;
        return true;
    }

    @SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            var p = server.getPlayerList().getPlayer(e.getKey());
            var j = e.getValue();
            if (p == null
                    || server.getTickCount() > j.expires
                    || !p.level().dimension().equals(j.dimension)
                    || !validOrigin(p, j.origin)) {
                it.remove();
                continue;
            }
            if (j.destination == null || server.getTickCount() < j.readyAt) continue;
            it.remove();
            var target = j.destination;
            var level = p.serverLevel();
            // Chunk access occurs only after 95 validated charge ticks for a ledger-owned
            // destination.
            level.getChunkAt(target);
            if (!level.getBlockState(target).is(WorldContent.BLOCKS.get("gate_core").get())) {
                GateLedger.get(level).remove(target);
                continue;
            }
            if (!level.getWorldBorder().isWithinBounds(target)
                    || !level.getBlockState(target.above())
                            .getCollisionShape(level, target.above())
                            .isEmpty()
                    || !level.getBlockState(target.above(2))
                            .getCollisionShape(level, target.above(2))
                            .isEmpty()
                    || !level.mayInteract(p, target)) continue;
            p.stopRiding();
            p.teleportTo(
                    level,
                    target.getX() + .5,
                    target.getY() + 1,
                    target.getZ() + .5,
                    p.getYRot(),
                    p.getXRot());
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            p.fallDistance = 0;
            var sound =
                    net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                            new net.minecraft.resources.ResourceLocation(
                                    "stellaeomphalos:gateway_teleport"));
            if (sound != null)
                p.playNotifySound(sound, net.minecraft.sounds.SoundSource.PLAYERS, .8F, 1);
        }
    }

    @SubscribeEvent
    public static void logout(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        SESSIONS.clear();
    }
}
