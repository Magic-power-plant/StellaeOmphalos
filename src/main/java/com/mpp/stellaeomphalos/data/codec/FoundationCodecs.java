package com.mpp.stellaeomphalos.data.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;

public final class FoundationCodecs {
    public static final Codec<Long> NONNEGATIVE_LONG = Codec.LONG.flatXmap(FoundationCodecs::nonnegative, FoundationCodecs::nonnegative);
    private FoundationCodecs() {}
    public static <T> MapCodec<T> optional(Codec<T> codec, String field, T fallback) {
        return new MapCodec<>() {
            @Override public <A> DataResult<T> decode(DynamicOps<A> ops, MapLike<A> input) {
                A value = input.get(field);
                return value == null ? DataResult.success(fallback) : codec.parse(ops, value).mapError(message -> "$." + field + ": " + message);
            }
            @Override public <A> RecordBuilder<A> encode(T input, DynamicOps<A> ops, RecordBuilder<A> prefix) {
                return java.util.Objects.equals(input, fallback) ? prefix : prefix.add(field, codec.encodeStart(ops, input));
            }
            @Override public <A> Stream<A> keys(DynamicOps<A> ops) { return Stream.of(ops.createString(field)); }
        };
    }
    private static DataResult<Long> nonnegative(long value) {
        return value < 0 ? DataResult.error(() -> "Expected a nonnegative integer") : DataResult.success(value);
    }
}
