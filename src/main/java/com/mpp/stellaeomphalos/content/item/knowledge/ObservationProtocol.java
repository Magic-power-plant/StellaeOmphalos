package com.mpp.stellaeomphalos.content.item.knowledge;

import com.mpp.stellaeomphalos.constellation.sign.*;
import com.mpp.stellaeomphalos.constellation.starmap.StarLine;
import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;
import com.mpp.stellaeomphalos.content.item.CatalogItems;
import com.mpp.stellaeomphalos.content.menu.StationMenus;
import com.mpp.stellaeomphalos.network.toServer.PktObserveSign;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.progress.StarRecords;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;

/** Validates the complete drawing, knowledge, active sky and the current observation instrument. */
public final class ObservationProtocol {
    private ObservationProtocol() {}

    public static boolean observe(ServerPlayer player, PktObserveSign packet) {
        var level = player.serverLevel();
        if (player instanceof net.minecraftforge.common.util.FakePlayer
                || !StarRecords.get(player).valid()
                || packet.session() != SignSkyService.sessionId(player)
                || !packet.dimension().equals(level.dimension().location())
                || !SignSkyService.visibleDimension(level)
                || !level.isNight()
                || level.isRaining()) return false;
        var sign = SignRegistry.byId(packet.sign());
        var progress = BoonProgress.getServer(player);
        if (sign == null
                || progress.knowsSign(sign.id())
                || !sign.canDiscover(player, progress)
                || !(progress.seenSigns().contains(sign.id())
                        || StarRecords.get(player).seenSigns().contains(sign.id()))
                || !SignSkyService.activeSigns(level).contains(sign)) return false;
        if (packet.handheld()) {
            if (!player.getMainHandItem().is(CatalogItems.HAND_SPYGLASS.get())
                    || player.getXRot() > -45
                    || !(sign instanceof MajorSign)
                    || !level.canSeeSky(player.blockPosition())) return false;
            var sorted =
                    SignSkyService.activeSigns(level).stream()
                            .sorted(
                                    java.util.Comparator.comparing(
                                            s -> s instanceof MajorSign ? 0 : 1))
                            .toList();
            if (!SignAim.aligned(
                    SignSkyAnchorTable.layout(sorted).get(sign),
                    player.getYRot(),
                    player.getXRot())) return false;
        } else {
            if (!(player.containerMenu instanceof StationMenus.StationMenu menu)
                    || menu instanceof StationMenus.StarChartTableMenu
                    || !menu.pos().equals(packet.origin())
                    || !menu.stillValid(player)
                    || !level.hasChunkAt(packet.origin())
                    || !level.canSeeSky(packet.origin().above(2))) return false;
        }
        if (!matches(packet.edges(), sign.lines())) return false;
        progress.discover(player, sign.id());
        com.mpp.stellaeomphalos.knowledge.advancement.AdvancementTriggers.discoverSign(
                player, sign.id());
        var sound =
                net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                        new net.minecraft.resources.ResourceLocation(
                                "stellaeomphalos:sign_discover"));
        if (sound != null)
            player.playNotifySound(sound, net.minecraft.sounds.SoundSource.PLAYERS, .7F, 1);
        return true;
    }

    public static boolean matches(
            java.util.List<PktObserveSign.Edge> proof, java.util.List<StarLine> template) {
        if (template.isEmpty() || proof.size() != template.size()) return false;
        var edges = new HashSet<StarLine>();
        try {
            for (var edge : proof)
                if (!edges.add(
                        new StarLine(
                                new StarPoint(edge.ax(), edge.ay()),
                                new StarPoint(edge.bx(), edge.by())))) return false;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        return edges.equals(new HashSet<>(template));
    }
}
