package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

public record PktThrottleNotice(int payloadId, long dropped) implements OmphalosPayload {
    public static final Codec<PktThrottleNotice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("payload_id").forGetter(PktThrottleNotice::payloadId),
            com.mpp.stellaeomphalos.data.codec.FoundationCodecs.NONNEGATIVE_LONG.fieldOf("dropped").forGetter(PktThrottleNotice::dropped)
    ).apply(instance, PktThrottleNotice::new));
}
