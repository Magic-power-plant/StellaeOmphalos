package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import java.util.*;

public record RetrogenStatusPayload(int processed, int queued, int skipped, boolean done)
        implements OmphalosPayload {
    public static final Codec<RetrogenStatusPayload> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("processed")
                                                    .forGetter(RetrogenStatusPayload::processed),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("queued")
                                                    .forGetter(RetrogenStatusPayload::queued),
                                            Codec.intRange(0, Integer.MAX_VALUE)
                                                    .fieldOf("skipped")
                                                    .forGetter(RetrogenStatusPayload::skipped),
                                            Codec.BOOL
                                                    .fieldOf("done")
                                                    .forGetter(RetrogenStatusPayload::done))
                                    .apply(i, RetrogenStatusPayload::new));
}
