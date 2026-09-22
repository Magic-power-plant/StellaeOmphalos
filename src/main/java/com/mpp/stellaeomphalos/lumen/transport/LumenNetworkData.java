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
 * Per-dimension lumen network save (key {@value #KEY}, schema version 2).
 * Unknown or unreadable saves are preserved read-only with a warning.
 * Topology edges, BFS cursors, epochs and pending queues are runtime-derived and never persisted.
 */
public final class LumenNetworkData extends VersionedSavedData<LumenNetworkData.LumenNetworkState> {
    public static final String KEY = "stellaeomphalos_lumen_network";
    public static final int VERSION = 2;
    private static final String DOMAIN = "stellaeomphalos_lumen_network";

    public record SavedSource(long pos, String provider, long output, Optional<String> sign,
                              boolean autoLink, boolean seesSky, boolean enhanced, double proximity, double noise) {
        public static final Codec<SavedSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("Pos").forGetter(SavedSource::pos),
                Codec.STRING.fieldOf("Provider").forGetter(SavedSource::provider),
                Codec.LONG.fieldOf("Output").forGetter(SavedSource::output),
                Codec.STRING.optionalFieldOf("Sign").forGetter(SavedSource::sign),
                Codec.BOOL.fieldOf("AutoLink").forGetter(SavedSource::autoLink),
                Codec.BOOL.fieldOf("SeesSky").forGetter(SavedSource::seesSky),
                Codec.BOOL.fieldOf("Enhanced").forGetter(SavedSource::enhanced),
                Codec.DOUBLE.optionalFieldOf("Proximity", 1.0).forGetter(SavedSource::proximity),
                Codec.DOUBLE.optionalFieldOf("Noise", 0.0).forGetter(SavedSource::noise))
                .apply(instance, SavedSource::new));
    }

    public record SavedNode(long pos, String io, String provider, long stored, long capacity, float loss) {
        public static final Codec<SavedNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("Pos").forGetter(SavedNode::pos),
                Codec.STRING.fieldOf("Io").forGetter(SavedNode::io),
                Codec.STRING.fieldOf("Provider").forGetter(SavedNode::provider),
                Codec.LONG.fieldOf("Stored").forGetter(SavedNode::stored),
                Codec.LONG.fieldOf("Capacity").forGetter(SavedNode::capacity),
                Codec.FLOAT.optionalFieldOf("Loss", 0.02f).forGetter(SavedNode::loss))
                .apply(instance, SavedNode::new));
    }

    public record LumenNetworkState(List<SavedSource> sources, List<SavedNode> nodes) {
        public static final LumenNetworkState EMPTY = new LumenNetworkState(List.of(), List.of());
        public static final Codec<LumenNetworkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SavedSource.CODEC.listOf().fieldOf("Sources").forGetter(LumenNetworkState::sources),
                SavedNode.CODEC.listOf().fieldOf("Nodes").forGetter(LumenNetworkState::nodes))
                .apply(instance, LumenNetworkState::new));
        public LumenNetworkState {
            sources = List.copyOf(sources);
            nodes = List.copyOf(nodes);
        }
    }

    private LumenNetworkData() {
        super(new MigrationChain(DOMAIN, VERSION, List.of(new com.mpp.stellaeomphalos.data.loader.DataMigrator() {
            public int fromVersion() { return 1; }
            public CompoundTag migrate(CompoundTag payload) {
                var result = new CompoundTag();
                for (String key : payload.getAllKeys()) {
                    var value = payload.get(key).copy();
                    if (value instanceof net.minecraft.nbt.ListTag list) {
                        for (var entry : list) if (entry instanceof CompoundTag fields) {
                            for (String field : java.util.List.copyOf(fields.getAllKeys())) {
                                var fieldValue = fields.get(field).copy();
                                fields.remove(field);
                                fields.put(Character.toUpperCase(field.charAt(0)) + field.substring(1), fieldValue);
                            }
                        }
                    }
                    result.put(Character.toUpperCase(key.charAt(0)) + key.substring(1), value);
                }
                return result;
            }
        })), LumenNetworkState.CODEC, "0.1.0", LumenNetworkState.EMPTY);
    }

    /** Replaces the stored snapshot and marks the data dirty; called by the owning network. */
    public void store(LumenNetworkState state) { update(state); }

    public static LumenNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(LumenNetworkData::load, LumenNetworkData::new, KEY);
    }

    static LumenNetworkData load(CompoundTag tag) {
        var data = new LumenNetworkData();
        data.restoreOrPreserve(tag, 0L);
        return data;
    }
}
