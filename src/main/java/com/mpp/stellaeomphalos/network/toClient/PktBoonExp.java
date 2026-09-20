package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

/**
 * Logical ID 18 (S2C): experience / level / available free points. Sent only when a value
 * actually changed, at most once per tick per player (the dispatcher batches dirty players).
 */
public record PktBoonExp(int sessionId, long exp, int level, int freePoints) implements OmphalosPayload {

    public static final Codec<PktBoonExp> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktBoonExp::sessionId),
            Codec.LONG.fieldOf("exp").forGetter(PktBoonExp::exp),
            Codec.INT.fieldOf("level").forGetter(PktBoonExp::level),
            Codec.INT.fieldOf("free_points").forGetter(PktBoonExp::freePoints)
    ).apply(instance, PktBoonExp::new));

    public PktBoonExp {
        if (exp < 0 || level < 1 || freePoints < 0) throw new IllegalArgumentException("negative boon progress value");
    }
}
