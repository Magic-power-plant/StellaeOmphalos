package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mpp.stellaeomphalos.data.codec.FoundationCodecs;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One language's naming lexicon (data/stellaeomphalos/sign_naming/&lt;lang&gt;.json).
 * Pools are addressed by the placeholders of the format template: zh uses {prefix}{core}{suffix},
 * en uses {onset}{body}{tail}; the middle pool repeats to reach the requested length.
 */
public record SignNamingLexicon(String format, Map<String, List<String>> pools, int minLength, int maxLength) {
    public static final Codec<SignNamingLexicon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("format").forGetter(SignNamingLexicon::format),
            FoundationCodecs.optional(Codec.STRING.listOf(), "prefix", List.<String>of()).forGetter(l -> l.pool("prefix")),
            FoundationCodecs.optional(Codec.STRING.listOf(), "core", List.<String>of()).forGetter(l -> l.pool("core")),
            FoundationCodecs.optional(Codec.STRING.listOf(), "suffix", List.<String>of()).forGetter(l -> l.pool("suffix")),
            FoundationCodecs.optional(Codec.STRING.listOf(), "onset", List.<String>of()).forGetter(l -> l.pool("onset")),
            FoundationCodecs.optional(Codec.STRING.listOf(), "body", List.<String>of()).forGetter(l -> l.pool("body")),
            FoundationCodecs.optional(Codec.STRING.listOf(), "tail", List.<String>of()).forGetter(l -> l.pool("tail")),
            FoundationCodecs.optional(Codec.INT, "min_length", -1).forGetter(SignNamingLexicon::minLength),
            FoundationCodecs.optional(Codec.INT, "max_length", -1).forGetter(SignNamingLexicon::maxLength),
            FoundationCodecs.optional(Codec.INT, "min_syllables", -1).forGetter(l -> -1),
            FoundationCodecs.optional(Codec.INT, "max_syllables", -1).forGetter(l -> -1)
    ).apply(instance, SignNamingLexicon::fromJson));

    @SuppressWarnings("unused")
    private static SignNamingLexicon fromJson(String format, List<String> prefix, List<String> core, List<String> suffix,
            List<String> onset, List<String> body, List<String> tail,
            int minLength, int maxLength, int minSyllables, int maxSyllables) {
        var pools = new LinkedHashMap<String, List<String>>();
        pools.put("prefix", prefix); pools.put("core", core); pools.put("suffix", suffix);
        pools.put("onset", onset); pools.put("body", body); pools.put("tail", tail);
        int min = minLength >= 0 ? minLength : minSyllables;
        int max = maxLength >= 0 ? maxLength : maxSyllables;
        return new SignNamingLexicon(format, pools, min, max);
    }

    public SignNamingLexicon {
        if (format.isBlank()) throw new IllegalArgumentException("Naming format is blank");
        var copy = new LinkedHashMap<String, List<String>>();
        pools.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        pools = java.util.Collections.unmodifiableMap(copy);
        if (minLength < 1) throw new IllegalArgumentException("Naming min length below 1");
        if (maxLength < minLength) throw new IllegalArgumentException("Naming max length below min length");
    }

    public List<String> pool(String name) {
        var pool = pools.get(name);
        if (pool == null || pool.isEmpty()) throw new IllegalArgumentException("Lexicon pool missing or empty: " + name);
        return pool;
    }
}
