package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.nbt.CompoundTag;

/** Append-only payload 40; business validation belongs to the receiving domain. */
public record PktStarRecordDelta(int session, long base, long revision, CompoundTag changes)
        implements OmphalosPayload {
    public static final Codec<PktStarRecordDelta> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.INT
                                                    .fieldOf("session")
                                                    .forGetter(PktStarRecordDelta::session),
                                            Codec.LONG
                                                    .fieldOf("base")
                                                    .forGetter(PktStarRecordDelta::base),
                                            Codec.LONG
                                                    .fieldOf("revision")
                                                    .forGetter(PktStarRecordDelta::revision),
                                            CompoundTag.CODEC
                                                    .fieldOf("changes")
                                                    .forGetter(PktStarRecordDelta::changes))
                                    .apply(i, PktStarRecordDelta::new));

    public PktStarRecordDelta {
        changes = changes.copy();
    }

    @Override
    public CompoundTag changes() {
        return changes.copy();
    }
}
