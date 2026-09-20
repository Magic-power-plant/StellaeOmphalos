package com.mpp.stellaeomphalos.lumen.transport.stasis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.data.loader.MigrationChain;
import com.mpp.stellaeomphalos.data.loader.VersionedSavedData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;

/** Per-dimension stasis persistence under key "stellaeomphalos_stasis" (schema: plan 2.4.2). */
public final class StasisData extends VersionedSavedData<StasisData.State> {
    public static final String KEY = "stellaeomphalos_stasis";
    private static final String WRITER_VERSION = "0.1.0";

    public record ZoneRecord(long center, float radius, String filterMode, Optional<UUID> owner,
                             boolean targetPlayers, long remaining, int particleTier) {
        public static final Codec<ZoneRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("Center").forGetter(ZoneRecord::center),
                Codec.FLOAT.fieldOf("Radius").forGetter(ZoneRecord::radius),
                Codec.STRING.fieldOf("FilterMode").forGetter(ZoneRecord::filterMode),
                UUIDUtil.STRING_CODEC.optionalFieldOf("Owner").forGetter(ZoneRecord::owner),
                Codec.BOOL.fieldOf("TargetPlayers").forGetter(ZoneRecord::targetPlayers),
                Codec.LONG.fieldOf("Remaining").forGetter(ZoneRecord::remaining),
                Codec.INT.fieldOf("ParticleTier").forGetter(ZoneRecord::particleTier)
        ).apply(instance, ZoneRecord::new));
    }

    public record State(List<ZoneRecord> zones) {
        public State { zones = List.copyOf(zones); }
        public static final Codec<State> CODEC = ZoneRecord.CODEC.listOf().fieldOf("Zones")
                .xmap(State::new, State::zones).codec();
    }

    public StasisData() {
        super(new MigrationChain(KEY, 1, List.of()), State.CODEC, WRITER_VERSION, new State(List.of()));
    }

    public static StasisData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(tag -> {
            var data = new StasisData();
            data.restore(tag, level.getGameTime());
            return data;
        }, StasisData::new, KEY);
    }

    public void store(State state) { update(state); }
}
