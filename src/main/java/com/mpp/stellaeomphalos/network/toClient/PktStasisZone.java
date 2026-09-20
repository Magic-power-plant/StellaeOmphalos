package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/** Logical ID 19 (S2C): batched stasis-zone upserts/removals, merged into one packet per tick. */
public record PktStasisZone(int sessionId, List<ZoneEntry> zones) implements OmphalosPayload {
    public enum Op {
        UPSERT, REMOVE;
        public String serialized() { return name().toLowerCase(Locale.ROOT); }
        public static final Codec<Op> CODEC = Codec.STRING.comapFlatMap(Op::parse, Op::serialized);
        private static DataResult<Op> parse(String text) {
            for (var op : values()) if (op.serialized().equals(text)) return DataResult.success(op);
            return DataResult.error(() -> "Unknown stasis zone op " + text);
        }
    }

    /** Wire-level copy of the stasis filter mode; local because network (layer 0) must not import lumen (layer 1). */
    public enum FilterMode {
        ALL_EXCEPT, NO_PLAYERS;
        public String serialized() { return name().toLowerCase(Locale.ROOT); }
        public static final Codec<FilterMode> CODEC = Codec.STRING.comapFlatMap(FilterMode::parse, FilterMode::serialized);
        private static DataResult<FilterMode> parse(String text) {
            for (var mode : values()) if (mode.serialized().equals(text)) return DataResult.success(mode);
            return DataResult.error(() -> "Unknown stasis filter mode " + text);
        }
    }

    public record ZoneEntry(Op op, long center, float radius, FilterMode filterMode, Optional<UUID> owner, int particleTier) {
        public static final Codec<ZoneEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Op.CODEC.fieldOf("op").forGetter(ZoneEntry::op),
                Codec.LONG.fieldOf("center").forGetter(ZoneEntry::center),
                Codec.FLOAT.fieldOf("radius").forGetter(ZoneEntry::radius),
                FilterMode.CODEC.fieldOf("filter_mode").forGetter(ZoneEntry::filterMode),
                UUIDUtil.STRING_CODEC.optionalFieldOf("owner").forGetter(ZoneEntry::owner),
                Codec.intRange(0, 255).fieldOf("particle_tier").forGetter(ZoneEntry::particleTier)
        ).apply(instance, ZoneEntry::new));
    }

    public PktStasisZone { zones = List.copyOf(zones); }

    public static final Codec<PktStasisZone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktStasisZone::sessionId),
            ZoneEntry.CODEC.listOf().fieldOf("zones").forGetter(PktStasisZone::zones)
    ).apply(instance, PktStasisZone::new));
}
