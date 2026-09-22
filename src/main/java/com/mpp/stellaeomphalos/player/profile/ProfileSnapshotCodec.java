package com.mpp.stellaeomphalos.player.profile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

/** Encodes versioned, integrity-checked profile snapshots without mutating the live profile. */
public final class ProfileSnapshotCodec {
    public static final int CURRENT_SCHEMA = 1;

    private ProfileSnapshotCodec() {}

    public static CompoundTag encode(PlayerProfile profile, UUID playerId, String serverFingerprint)
            throws IOException {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(serverFingerprint, "serverFingerprint");
        var snapshot = new CompoundTag();
        snapshot.putInt("schema", CURRENT_SCHEMA);
        snapshot.putString("server_fingerprint", serverFingerprint);
        snapshot.putString("player_soft_id", playerId.toString());
        snapshot.put("payload", profile.save());
        snapshot.putString("checksum", checksum(snapshot));
        return snapshot;
    }

    /**
     * Validates a snapshot and returns a detached profile. The caller can decide whether a
     * server/identity mismatch is acceptable before copying this detached value into the live
     * capability.
     */
    public static DefaultPlayerProfile decode(
            CompoundTag snapshot,
            UUID targetPlayer,
            String serverFingerprint,
            boolean force) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.getInt("schema") != CURRENT_SCHEMA
                || !snapshot.contains("payload", Tag.TAG_COMPOUND)
                || snapshot.getString("server_fingerprint").isBlank()
                || snapshot.getString("player_soft_id").isBlank()
                || snapshot.getString("checksum").isBlank()) {
            throw new IllegalArgumentException("Unsupported profile snapshot");
        }
        if (!MessageDigest.isEqual(
                snapshot.getString("checksum").getBytes(StandardCharsets.US_ASCII),
                checksum(snapshot).getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("Profile snapshot checksum mismatch");
        }
        boolean sameServer = serverFingerprint.equals(snapshot.getString("server_fingerprint"));
        boolean samePlayer = targetPlayer.toString().equals(snapshot.getString("player_soft_id"));
        if (!force && (!sameServer || !samePlayer)) {
            throw new IllegalArgumentException("Profile snapshot identity mismatch");
        }
        var detached = new DefaultPlayerProfile();
        detached.load(snapshot.getCompound("payload"));
        return detached;
    }

    public static String serverFingerprint(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return sha256(server.getWorldData().getLevelName() + "\n" + server.overworld().getSeed());
    }

    public static String checksum(CompoundTag snapshot) {
        try {
            var payload = snapshot.getCompound("payload");
            var bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(payload, bytes);
            var identity = snapshot.getInt("schema")
                    + "\n"
                    + snapshot.getString("server_fingerprint")
                    + "\n"
                    + snapshot.getString("player_soft_id")
                    + "\n";
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(identity.getBytes(StandardCharsets.UTF_8));
            digest.update(bytes.toByteArray());
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to checksum profile snapshot", exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}