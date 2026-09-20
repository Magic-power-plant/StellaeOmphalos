package com.mpp.stellaeomphalos.constellation.sign;

import java.util.Random;

/** English naming: {onset}{body}{tail}; length counts syllables, extra syllables repeat the body. */
public final class SignNamingProviderEn extends SignNamingProvider {
    @Override public boolean supports(String languageCode) { return true; }

    @Override public String generate(SignNamingLexicon lexicon, Random random) {
        int syllables = lexicon.minLength()
                + (lexicon.maxLength() > lexicon.minLength() ? random.nextInt(lexicon.maxLength() - lexicon.minLength() + 1) : 0);
        return compose(lexicon, random, "body", Math.max(0, syllables - 2));
    }
}
