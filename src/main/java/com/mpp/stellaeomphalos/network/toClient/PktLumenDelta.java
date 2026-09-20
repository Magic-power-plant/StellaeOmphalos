package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.core.BlockPos;

/** ID 21 (S2C): a node's stored LU changed (&gt;5% of capacity, at most once per node per 2 s). */
public record PktLumenDelta(int sessionId, BlockPos pos, long stored, long capacity) implements OmphalosPayload {
    public static final Codec<PktLumenDelta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktLumenDelta::sessionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(PktLumenDelta::pos),
            Codec.LONG.fieldOf("stored").forGetter(PktLumenDelta::stored),
            Codec.LONG.fieldOf("capacity").forGetter(PktLumenDelta::capacity))
            .apply(instance, PktLumenDelta::new));
    public PktLumenDelta {
        pos = pos.immutable();
        if (stored < 0 || capacity < 0) throw new IllegalArgumentException("Negative lumen state");
    }
}
