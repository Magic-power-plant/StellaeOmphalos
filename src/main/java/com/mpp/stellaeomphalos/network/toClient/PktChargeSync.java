package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

/** Logical ID 10 (S2C): server-authoritative starlight charge, quantized to 1/256. */
public record PktChargeSync(int sessionId, int quantizedCharge) implements OmphalosPayload {
    public static final int QUANTUM = 256;
    public static final Codec<PktChargeSync> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktChargeSync::sessionId),
            Codec.intRange(0, QUANTUM).fieldOf("quantized_charge").forGetter(PktChargeSync::quantizedCharge)
    ).apply(instance, PktChargeSync::new));
}
