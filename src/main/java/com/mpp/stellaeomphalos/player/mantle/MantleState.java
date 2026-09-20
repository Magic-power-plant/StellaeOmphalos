package com.mpp.stellaeomphalos.player.mantle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Serialized value, separate from an effect session's lifetime. */
public record MantleState(ResourceLocation effect, CompoundTag data) {
    public static final Codec<MantleState> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            ResourceLocation.CODEC
                                                    .fieldOf("Effect")
                                                    .forGetter(MantleState::effect),
                                            CompoundTag.CODEC
                                                    .fieldOf("Data")
                                                    .forGetter(MantleState::data))
                                    .apply(i, MantleState::new));

    public MantleState {
        data = data.copy();
    }

    @Override
    public CompoundTag data() {
        return data.copy();
    }
}
