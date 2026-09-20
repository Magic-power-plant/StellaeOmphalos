package com.mpp.stellaeomphalos.structure.match;

import com.mpp.stellaeomphalos.structure.pattern.*;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public final class StructureWatch extends AbstractStructureWatch {
    private final ServerLevel level;
    private final PlacementTransform transform;
    private StructureAuthority authority;
    private int epoch = -1;
    private long verified;
    private StructureState last = StructureState.INDETERMINATE;

    public StructureWatch(
            ServerLevel level, BlockPos origin, ResourceLocation id, PlacementTransform transform) {
        super(origin, id);
        this.level = level;
        this.transform = transform;
        refresh();
    }

    public void refresh() {
        if (epoch == BlueprintRegistry.epoch()) return;
        epoch = BlueprintRegistry.epoch();
        authority =
                BlueprintRegistry.find(blueprintId)
                        .map(b -> new StructureAuthority(b.transformed(transform), origin))
                        .orElse(null);
        if (authority != null) authority.initialize(level);
        verified = level.getGameTime();
    }

    public Set<ChunkPos> chunks() {
        return authority == null ? Set.of(new ChunkPos(origin)) : authority.chunks();
    }

    public StructureAuthority authority() {
        return authority;
    }

    public StructureState state() {
        return closed() || authority == null ? StructureState.LOCKED : authority.state();
    }

    public PlacementTransform transform() {
        return transform;
    }

    public void changed(BlockPos pos) {
        if (authority != null) authority.changed(level, pos);
    }

    public void chunk(ChunkPos pos, boolean loaded) {
        if (authority != null) {
            if (loaded) authority.verifyChunk(level, pos);
            else authority.unload(pos);
        }
        publish();
    }

    public void verify() {
        if (authority != null) authority.initialize(level);
        verified = level.getGameTime();
        publish();
    }

    public void tick(int interval) {
        if (level.getGameTime() - verified >= interval) verify();
        else publish();
    }

    public void publish() {
        var now = state();
        if (now == last) return;
        var old = last;
        last = now;
        if (level.hasChunkAt(origin)
                && level.getBlockEntity(origin) instanceof StructureDependent dependent)
            dependent.onStructureStateChanged(now, old);
        if (now.canProduce() || now == StructureState.BROKEN) {
            var sound =
                    net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                            new ResourceLocation(
                                    "stellaeomphalos",
                                    now.canProduce() ? "structure_formed" : "structure_break"));
            if (sound != null)
                level.playSound(
                        null, origin, sound, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1);
        }
        var packet =
                new com.mpp.stellaeomphalos.network.toClient.StructureStatePayload(
                        origin,
                        now.ordinal(),
                        authority == null ? 0 : (int) (100 * authority.completeness()),
                        authority == null ? 0 : authority.degradations().size());
        for (var player : level.players())
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(origin)) <= 1024)
                com.mpp.stellaeomphalos.network.OmphalosChannel.send(player, packet);
    }

    protected void detach() {
        authority = null;
    }
}
