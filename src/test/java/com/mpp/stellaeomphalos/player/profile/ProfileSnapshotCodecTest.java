package com.mpp.stellaeomphalos.player.profile;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileSnapshotCodecTest {
    private static final UUID PLAYER = UUID.fromString("12345678-1234-5678-1234-567812345678");

    @Test
    void snapshotRoundTripCarriesIdentityAndPayload() throws Exception {
        var profile = new DefaultPlayerProfile();
        profile.discoverSign(new ResourceLocation("stellaeomphalos", "aevitas"));
        profile.grantResearch(new ResourceLocation("stellaeomphalos", "chapter/discovery"));

        var snapshot = ProfileSnapshotCodec.encode(profile, PLAYER, "server-a");
        var restored = ProfileSnapshotCodec.decode(snapshot, PLAYER, "server-a", false);

        assertEquals(profile.save(), restored.save());
        assertEquals(ProfileSnapshotCodec.CURRENT_SCHEMA, snapshot.getInt("schema"));
        assertEquals(64, snapshot.getString("checksum").length());
    }

    @Test
    void checksumAndIdentityAreCheckedBeforeReturningDetachedProfile() throws Exception {
        var profile = new DefaultPlayerProfile();
        profile.grantExperience(40);
        var snapshot = ProfileSnapshotCodec.encode(profile, PLAYER, "server-a");

        snapshot.getCompound("payload").putLong("boon_exp", 999);
        assertThrows(IllegalArgumentException.class,
                () -> ProfileSnapshotCodec.decode(snapshot, PLAYER, "server-a", false));

        var intact = ProfileSnapshotCodec.encode(profile, PLAYER, "server-a");
        assertThrows(IllegalArgumentException.class,
                () -> ProfileSnapshotCodec.decode(intact, UUID.randomUUID(), "server-a", false));
        assertEquals(profile.save(),
                ProfileSnapshotCodec.decode(intact, UUID.randomUUID(), "server-b", true).save());
    }

    @Test
    void malformedSnapshotIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ProfileSnapshotCodec.decode(new CompoundTag(), PLAYER, "server-a", false));
    }
}