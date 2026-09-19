package com.mpp.stellaeomphalos.core.util.item;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

public final class StackNbt {
    private StackNbt() {}
    public static <T> Optional<T> read(ItemStack stack, String key, Codec<T> codec) {
        var tag = stack.getTag();
        return tag == null || !tag.contains(key) ? Optional.empty() : codec.parse(NbtOps.INSTANCE, tag.get(key)).result();
    }
    public static <T> void write(ItemStack stack, String key, Codec<T> codec, T value) {
        if (!key.matches("[A-Z][A-Za-z0-9]*")) throw new IllegalArgumentException("Invalid NBT key");
        var encoded = codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, message -> { throw new IllegalArgumentException(message); });
        CompoundTag tag = stack.getOrCreateTag(); tag.put(key, encoded);
    }
}
