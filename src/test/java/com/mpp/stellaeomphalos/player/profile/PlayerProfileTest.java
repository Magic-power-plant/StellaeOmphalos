package com.mpp.stellaeomphalos.player.profile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileTest {
    private static ResourceLocation id(String path) {
        return new ResourceLocation("stellaeomphalos", path);
    }

    @Test
    void missingFieldsUseDefaultsAndKnownSignsAreSeen() {
        var profile = new DefaultPlayerProfile();
        profile.load(new CompoundTag());
        assertTrue(profile.knownSigns().isEmpty());
        assertTrue(profile.seenSigns().isEmpty());
        assertEquals(0, profile.freeTokens());

        profile.discoverSign(id("aevitas"));
        assertTrue(profile.knownSigns().contains(id("aevitas")));
        assertTrue(profile.seenSigns().contains(id("aevitas")));
    }

    @Test
    void nbtRoundTripAndCopyPreserveAuthoritativeFields() {
        var source = new DefaultPlayerProfile();
        source.discoverSign(id("aevitas"));
        source.markSeen(id("armara"));
        source.grantResearch(id("foundation"));
        source.setAttuned(id("aevitas"));
        source.grantExperience(120);
        source.setFreeTokens(4);

        var restored = new DefaultPlayerProfile();
        restored.load(source.save());
        assertEquals(source.knownSigns(), restored.knownSigns());
        assertEquals(source.seenSigns(), restored.seenSigns());
        assertEquals(source.researchGroups(), restored.researchGroups());
        assertEquals(source.attunedSign(), restored.attunedSign());
        assertEquals(source.boonExperience(), restored.boonExperience());
        assertEquals(source.freeTokens(), restored.freeTokens());

        var copied = new DefaultPlayerProfile();
        copied.copyFrom(source);
        assertEquals(source.save(), copied.save());
    }

    @Test
    void collectionSizeIsBoundedAndResetClearsProgress() {
        var profile = new DefaultPlayerProfile();
        for (int i = 0; i < 2050; i++) profile.discoverSign(id("sign_" + i));
        assertEquals(2048, profile.knownSigns().size());
        assertEquals(2048, profile.seenSigns().size());
        profile.grantResearch(id("foundation"));
        profile.resetProgress();
        assertTrue(profile.knownSigns().isEmpty());
        assertTrue(profile.researchGroups().isEmpty());
        assertNull(profile.attunedSign());
    }
}
