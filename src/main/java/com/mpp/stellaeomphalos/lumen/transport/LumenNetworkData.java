package com.mpp.stellaeomphalos.lumen.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.data.loader.MigrationChain;
import com.mpp.stellaeomphalos.data.loader.VersionedSavedData;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Per-dimension lumen network save (key {@value #KEY}, schema version 1).
 * Unknown or unreadable versions are dropped and rebuilt with a warning, never thrown.
 * Topology edges, BFS cursors, epochs and pending queues are runtime-derived and never persisted.
 */
public final class LumenNetworkData extends VersionedSavedData<LumenNetworkData.LumenNetworkState> {
    public static final String KEY = "stellaeomphalos_lumen_network";
    public static final int VERSION = 1;
    private static final String DOMAIN = "stellaeomphalos_lumen_network";

    public record SavedSource(long pos, String provider, long output, Optional<String> sign,
                              boolean autoLink, boolean seesSky, boolean enhanced, double proximity, double noise) {
        public static final Codec<SavedSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(SavedSource::pos),
                Codec.STRING.fieldOf("provider").forGetter(SavedSource::provider),
                Codec.LONG.fieldOf("output").forGetter(SavedSource::output),
                Codec.STRING.optionalFieldOf("sign").forGetter(SavedSource::sign),
                Codec.BOOL.fieldOf("autoLink").forGetter(SavedSource::autoLink),
                Codec.BOOL.fieldOf("seesSky").forGetter(SavedSource::seesSky),
                Codec.BOOL.fieldOf("enhanced").forGetter(SavedSource::enhanced),
                Codec.DOUBLE.optionalFieldOf("proximity", 1.0).forGetter(SavedSource::proximity),
                Codec.DOUBLE.optionalFieldOf("noise", 0.0).forGetter(SavedSource::noise))
                .apply(instance, SavedSource::new));
    }

    public record SavedNode(long pos, String io, String provider, long stored, long capacity, float loss) {
        public static final Codec<SavedNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("pos").forGetter(SavedNode::pos),
                Codec.STRING.fieldOf("io").forGetter(SavedNode::io),
                Codec.STRING.fieldOf("provider").forGetter(SavedNode::provider),
                Codec.LONG.fieldOf("stored").forGetter(SavedNode::stored),
                Codec.LONG.fieldOf("capacity").forGetter(SavedNode::capacity),
                Codec.FLOAT.optionalFieldOf("loss", 0.02f).forGetter(SavedNode::loss))
                .apply(instance, SavedNode::new));
    }

    public record LumenNetworkState(List<SavedSource> sources, List<SavedNode> nodes) {
        public static final LumenNetworkState EMPTY = new LumenNetworkState(List.of(), List.of());
        public static final Codec<LumenNetworkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SavedSource.CODEC.listOf().fieldOf("sources").forGetter(LumenNetworkState::sources),
                SavedNode.CODEC.listOf().fieldOf("nodes").forGetter(LumenNetworkState::nodes))
                .apply(instance, LumenNetworkState::new));
        public LumenNetworkState {
            sources = List.copyOf(sources);
            nodes = List.copyOf(nodes);
        }
    }

    private LumenNetworkData() {
        super(new MigrationChain(DOMAIN, VERSION, List.of()), LumenNetworkState.CODEC, "0.1.0", LumenNetworkState.EMPTY);
    }

    /** Replaces the stored snapshot and marks the data dirty; called by the owning network. */
    public void store(LumenNetworkState state) { update(state); }

    public static LumenNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LumenNetworkData::load, LumenNetworkData::new, KEY);
    }

    private static LumenNetworkData load(CompoundTag tag) {
        var data = new LumenNetworkData();
        try {
            data.restore(tag, 0L);
        } catch (RuntimeException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Discarding unreadable lumen network save ({}); rebuilding empty", exception.getMessage());
        }
        return data;
    }
}
