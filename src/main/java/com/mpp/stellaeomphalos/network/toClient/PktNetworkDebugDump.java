package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record PktNetworkDebugDump(BlockPos position, CompoundTag data) implements OmphalosPayload {
    public static final Codec<PktNetworkDebugDump> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("position").forGetter(PktNetworkDebugDump::position),
            CompoundTag.CODEC.fieldOf("data").forGetter(PktNetworkDebugDump::data)).apply(instance, PktNetworkDebugDump::new));
    public PktNetworkDebugDump { position = position.immutable(); data = data.copy(); }
    @Override public CompoundTag data() { return data.copy(); }
}
