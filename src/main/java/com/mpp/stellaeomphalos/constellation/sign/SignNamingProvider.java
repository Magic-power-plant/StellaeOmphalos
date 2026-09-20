package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.serialization.JsonOps;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.data.codec.BoundedJson;
import com.mpp.stellaeomphalos.data.loader.DataBootstrap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;

/**
 * Deterministic random sign naming. Dispatch by language code with fallback to en_us; lexicons come
 * from the sign_naming data table when loaded (server / integrated client), otherwise from the copy
 * shipped inside the mod jar (dedicated-server clients). Generation is {@code new Random(seed)}
 * deterministic: same seed + same language always yields the same name. Callers on the client must
 * resolve the language only after the Minecraft instance exists.
 */
public abstract class SignNamingProvider {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)\\}");
    private static final List<SignNamingProvider> PROVIDERS = List.of(new SignNamingProviderZh(), new SignNamingProviderEn());
    private static final SignNamingProvider ENGLISH = new SignNamingProviderEn();

    /** Generates a name for the template. */
    public abstract String generate(SignNamingLexicon lexicon, Random random);
    /** Language codes this provider handles (lowercase, e.g. "zh_cn"). */
    public abstract boolean supports(String languageCode);

    public static String generateName(String languageCode, long seed) {
        String code = languageCode == null ? "en_us" : languageCode.toLowerCase(Locale.ROOT);
        return providerFor(code).generate(lexicon(code), new Random(seed));
    }

    static SignNamingProvider providerFor(String code) {
        for (var provider : PROVIDERS) if (provider.supports(code)) return provider;
        return ENGLISH;
    }

    static SignNamingLexicon lexicon(String code) {
        var lexicon = fromTable(code).or(() -> fromJar(code));
        if (lexicon.isEmpty() && !code.equals("en_us")) lexicon = fromTable("en_us").or(() -> fromJar("en_us"));
        return lexicon.orElseThrow(() -> new IllegalStateException("No sign naming lexicon for en_us"));
    }

    private static Optional<SignNamingLexicon> fromTable(String code) {
        return Optional.ofNullable(DataBootstrap.TABLES.entries(SignBootstrap.SIGN_NAMING)
                .get(new ResourceLocation(Omphalos.MODID, code)));
    }

    private static Optional<SignNamingLexicon> fromJar(String code) {
        String path = "/data/" + Omphalos.MODID + "/sign_naming/" + code + ".json";
        try (var stream = SignNamingProvider.class.getResourceAsStream(path)) {
            if (stream == null) return Optional.empty();
            var json = BoundedJson.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return SignNamingLexicon.CODEC.parse(JsonOps.INSTANCE, json).result();
        } catch (java.io.IOException | RuntimeException exception) {
            return Optional.empty();
        }
    }

    /** Substitutes each {pool} placeholder once, except the repeating middle pool. */
    protected static String compose(SignNamingLexicon lexicon, Random random, String repeatingPool, int repeatCount) {
        var result = new StringBuilder();
        var matcher = PLACEHOLDER.matcher(lexicon.format());
        while (matcher.find()) {
            String pool = matcher.group(1);
            int count = pool.equals(repeatingPool) ? Math.max(0, repeatCount) : 1;
            for (int i = 0; i < count; i++) result.append(pick(lexicon.pool(pool), random));
        }
        return result.toString();
    }

    protected static String pick(List<String> pool, Random random) {
        return pool.get(random.nextInt(pool.size()));
    }
}
