package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import com.mpp.stellaeomphalos.network.toServer.PktSkySeedRequest;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** S2C (payload id 13): the day's active sign set, as (numeric sign id, distribution) pairs. */
public record PktActiveSigns(int sessionId, ResourceKey<Level> dim, int day, List<Entry> signs) implements OmphalosPayload {
    public record Entry(int sign, float dist) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("sign").forGetter(Entry::sign),
                Codec.FLOAT.fieldOf("dist").forGetter(Entry::dist)
        ).apply(instance, Entry::new));
    }

    public static final Codec<PktActiveSigns> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktActiveSigns::sessionId),
            PktSkySeedRequest.DIMENSION.fieldOf("dim").forGetter(PktActiveSigns::dim),
            Codec.INT.fieldOf("day").forGetter(PktActiveSigns::day),
            Entry.CODEC.listOf().fieldOf("signs").forGetter(PktActiveSigns::signs)
    ).apply(instance, PktActiveSigns::new));

    public PktActiveSigns { signs = List.copyOf(signs); }
}
