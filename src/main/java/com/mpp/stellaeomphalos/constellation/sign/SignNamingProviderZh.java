package com.mpp.stellaeomphalos.constellation.sign;

import java.util.Locale;
import java.util.Random;

/** Chinese naming: {prefix}{core}{suffix}; length counts segments, extra length repeats the core. */
public final class SignNamingProviderZh extends SignNamingProvider {
    @Override public boolean supports(String languageCode) {
        return languageCode.toLowerCase(Locale.ROOT).startsWith("zh");
    }

    @Override public String generate(SignNamingLexicon lexicon, Random random) {
        int segments = lexicon.minLength()
                + (lexicon.maxLength() > lexicon.minLength() ? random.nextInt(lexicon.maxLength() - lexicon.minLength() + 1) : 0);
        return compose(lexicon, random, "core", Math.max(1, segments - 2));
    }
}
