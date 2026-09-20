package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;

import net.minecraft.nbt.CompoundTag;

/** Append-only payload 45; business validation belongs to the receiving domain. */
public record PktMantleState(String player, CompoundTag data) implements OmphalosPayload {
    public static final Codec<PktMantleState> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.STRING
                                                    .fieldOf("player")
                                                    .forGetter(PktMantleState::player),
                                            CompoundTag.CODEC
                                                    .fieldOf("data")
                                                    .forGetter(PktMantleState::data))
                                    .apply(i, PktMantleState::new));

    public PktMantleState {
        data = data.copy();
    }

    @Override
    public CompoundTag data() {
        return data.copy();
    }
}
