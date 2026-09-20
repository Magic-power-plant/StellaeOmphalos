package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import java.util.Optional;

/**
 * Logical ID 24 (S2C): one domain particle request. {@code type} selects the effect family
 * (see {@link Types}), {@code pos} the source position, {@code target} an optional second position
 * (e.g. projectile reflect target), {@code seed} the per-effect random seed so every client renders
 * the identical burst. High-frequency senders must pass through the throttles of the domain module
 * (smelting merge 1/tick, acceleration 10-tick interval, spawn warmup >= 5 ticks).
 */
public record PktDomainParticle(int sessionId, int type, long pos, Optional<Long> target, long seed)
        implements OmphalosPayload {

    /** Wire-level type ids; the semantic mapping is fixed by the domain module. */
    public static final class Types {
        public static final int VERDANCE = 0;
        public static final int AEGIS = 1;
        public static final int HERD = 2;
        public static final int SEVERANCE = 3;
        public static final int UPHEAVAL = 4;
        public static final int CRUCIBLE = 5;
        public static final int CHRONOS = 6;
        public static final int LUMINA = 7;
        public static final int PETROGENESIS = 8;
        public static final int ANGLING = 9;
        public static final int SPAWN = 10;
        public static final int SOAR = 11;
        public static final int METEOR_STRIKE = 12;
        public static final int METEOR_IMPACT = 13;
        private Types() {}
    }

    public static final Codec<PktDomainParticle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktDomainParticle::sessionId),
            Codec.INT.fieldOf("type").forGetter(PktDomainParticle::type),
            Codec.LONG.fieldOf("pos").forGetter(PktDomainParticle::pos),
            Codec.LONG.optionalFieldOf("target").forGetter(PktDomainParticle::target),
            Codec.LONG.fieldOf("seed").forGetter(PktDomainParticle::seed)
    ).apply(instance, PktDomainParticle::new));
}
