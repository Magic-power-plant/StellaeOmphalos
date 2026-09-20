package com.mpp.stellaeomphalos.constellation.domain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.data.loader.MigrationChain;
import com.mpp.stellaeomphalos.data.loader.VersionedSavedData;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Simplified star-structure ledger (plan 2.7.15: worldgen itself belongs to Part-4): per dimension,
 * structure type id -> known positions, plus a nearest-distance query used to space structures.
 * Persisted per dimension under {@code stellaeomphalos_star_structures}.
 */
public final class StarStructureLedger extends VersionedSavedData<StarStructureLedger.State> {
    public static final String KEY = "stellaeomphalos_star_structures";
    private static final String WRITER_VERSION = "0.1.0";
    /** Simplified cap per structure type; oldest entries are evicted. */
    private static final int MAX_PER_TYPE = 256;

    public record State(Map<String, List<Long>> structures) {
        public State { structures = deepCopy(structures); }
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf()).fieldOf("Structures").forGetter(State::structures)
        ).apply(instance, State::new));
        private static Map<String, List<Long>> deepCopy(Map<String, List<Long>> input) {
            var copy = new java.util.LinkedHashMap<String, List<Long>>();
            input.forEach((type, positions) -> copy.put(type, List.copyOf(positions)));
            return Map.copyOf(copy);
        }
    }

    public StarStructureLedger() {
        super(new MigrationChain(KEY, 1, List.of()), State.CODEC, WRITER_VERSION, new State(Map.of()));
    }

    public static StarStructureLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(tag -> {
            var ledger = new StarStructureLedger();
            ledger.restore(tag, level.getGameTime());
            return ledger;
        }, StarStructureLedger::new, KEY);
    }

    /** Records a known structure position (deduplicated, capped per type). */
    public void record(String structureType, BlockPos pos) {
        var map = new java.util.LinkedHashMap<>(state().structures());
        var positions = new java.util.ArrayList<>(map.getOrDefault(structureType, List.of()));
        if (!positions.contains(pos.asLong())) {
            positions.add(pos.asLong());
            while (positions.size() > MAX_PER_TYPE) positions.remove(0);
        }
        map.put(structureType, positions);
        update(new State(map));
    }

    /** Distance (blocks) to the nearest recorded structure of the type; empty when none recorded. */
    public OptionalDouble nearestDistance(String structureType, BlockPos pos) {
        var positions = state().structures().get(structureType);
        if (positions == null || positions.isEmpty()) return OptionalDouble.empty();
        double best = Double.MAX_VALUE;
        for (long packed : positions) best = Math.min(best, Math.sqrt(pos.distSqr(BlockPos.of(packed))));
        return OptionalDouble.of(best);
    }

    public void store(State state) { update(state); }
}
