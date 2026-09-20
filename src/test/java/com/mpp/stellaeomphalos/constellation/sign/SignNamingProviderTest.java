package com.mpp.stellaeomphalos.constellation.sign;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SignNamingProviderTest {
    @Test void sameSeedSameLanguageYieldsSameName() {
        assertEquals(SignNamingProvider.generateName("zh_cn", 42L), SignNamingProvider.generateName("zh_cn", 42L));
        assertEquals(SignNamingProvider.generateName("en_us", 42L), SignNamingProvider.generateName("en_us", 42L));
    }

    @Test void unknownLanguageFallsBackToEnglish() {
        assertEquals(SignNamingProvider.generateName("en_us", 7L), SignNamingProvider.generateName("fr_fr", 7L));
        assertEquals(SignNamingProvider.generateName("en_us", 7L), SignNamingProvider.generateName("klingon", 7L));
    }

    @Test void zhProviderOnlyClaimsChineseCodes() {
        assertTrue(SignNamingProvider.providerFor("zh_cn") instanceof SignNamingProviderZh);
        assertTrue(SignNamingProvider.providerFor("zh_tw") instanceof SignNamingProviderZh);
        assertTrue(SignNamingProvider.providerFor("en_us") instanceof SignNamingProviderEn);
        assertTrue(SignNamingProvider.providerFor("ja_jp") instanceof SignNamingProviderEn);
    }

    @Test void zhNameFollowsPrefixCoreSuffixTemplate() {
        var lexicon = SignNamingProvider.lexicon("zh_cn");
        assertEquals("{prefix}{core}{suffix}", lexicon.format());
        for (long seed = 0; seed < 64; seed++) {
            String name = SignNamingProvider.generateName("zh_cn", seed);
            assertFalse(name.isBlank());
            assertTrue(lexicon.pool("prefix").stream().anyMatch(name::startsWith), name);
            assertTrue(lexicon.pool("suffix").stream().anyMatch(name::endsWith), name);
        }
    }

    @Test void enNameFollowsOnsetBodyTailTemplate() {
        var lexicon = SignNamingProvider.lexicon("en_us");
        var seen = new HashSet<String>();
        for (long seed = 0; seed < 64; seed++) {
            String name = SignNamingProvider.generateName("en_us", seed);
            assertFalse(name.isBlank());
            assertTrue(lexicon.pool("onset").stream().anyMatch(name::startsWith), name);
            assertTrue(lexicon.pool("tail").stream().anyMatch(name::endsWith), name);
            seen.add(name);
        }
        assertTrue(seen.size() > 16, "name pool too small: " + seen.size());
    }

    @Test void providersRespectInjectedLexicons() {
        var lexicon = new SignNamingLexicon("{a}{b}{c}", java.util.Map.of(
                "a", java.util.List.of("X"), "b", java.util.List.of("y", "z"), "c", java.util.List.of("Q")), 2, 2);
        var provider = new SignNamingProvider() {
            @Override public boolean supports(String languageCode) { return true; }
            @Override public String generate(SignNamingLexicon lex, Random random) { return compose(lex, random, "b", Math.max(0, 2 - 2)); }
        };
        assertEquals("XQ", provider.generate(lexicon, new Random(1L)));
    }
}
