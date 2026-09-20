package com.mpp.stellaeomphalos.network.toServer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.core.BlockPos;

/** ID 22 (C2S): a GUI opened on a receiving node asks for its current stored/capacity snapshot. */
public record PktLumenSinkQuery(int sessionId, BlockPos pos) implements OmphalosPayload {
    public static final Codec<PktLumenSinkQuery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktLumenSinkQuery::sessionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(PktLumenSinkQuery::pos))
            .apply(instance, PktLumenSinkQuery::new));
    public PktLumenSinkQuery { pos = pos.immutable(); }
}
