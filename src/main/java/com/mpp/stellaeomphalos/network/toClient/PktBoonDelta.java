package com.mpp.stellaeomphalos.network.toClient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.network.OmphalosPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Logical ID 17 (S2C): single-node progress delta (unlock / remove / seal / unseal / socket
 * change). At most one packet per operation. {@code extra} carries the socketed item tag for
 * {@link #ACTION_SOCKET} and is empty otherwise.
 */
public record PktBoonDelta(int sessionId, ResourceLocation id, byte action, CompoundTag extra) implements OmphalosPayload {

    public static final byte ACTION_UNLOCK = 0;
    public static final byte ACTION_REMOVE = 1;
    public static final byte ACTION_SEAL = 2;
    public static final byte ACTION_UNSEAL = 3;
    public static final byte ACTION_SOCKET = 4;

    public static final Codec<PktBoonDelta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("session_id").forGetter(PktBoonDelta::sessionId),
            ResourceLocation.CODEC.fieldOf("id").forGetter(PktBoonDelta::id),
            Codec.BYTE.fieldOf("action").forGetter(PktBoonDelta::action),
            CompoundTag.CODEC.fieldOf("extra").forGetter(PktBoonDelta::extra)
    ).apply(instance, PktBoonDelta::new));

    public PktBoonDelta {
        if (action < ACTION_UNLOCK || action > ACTION_SOCKET) throw new IllegalArgumentException("bad delta action " + action);
        extra = extra.copy();
    }
}
