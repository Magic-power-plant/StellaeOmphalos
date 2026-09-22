package com.mpp.stellaeomphalos.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Each peer owns at most one bounded, fixed-deadline reassembly session. */
public final class ChunkAssembler {
    public record Completed(int payloadId, byte[] bytes) {}
    public static final long TIMEOUT_NANOS = 3000000000L;
    private static final class Session {
        final ChunkedEnvelope header;
        final byte[][] chunks;
        final long deadline;
        int count;
        Session(ChunkedEnvelope header, long now) {
            this.header = header; chunks = new byte[header.chunks()][]; deadline = now + TIMEOUT_NANOS;
        }
    }
    private final Map<UUID, Session> sessions = new HashMap<>();
    public Optional<Completed> accept(UUID peer, ChunkedEnvelope fragment, long now) {
        expire(now);
        var active = sessions.computeIfAbsent(peer, ignored -> new Session(fragment, now));
        var header = active.header;
        if (!header.session().equals(fragment.session())) {
            sessions.remove(peer);
            throw new IllegalArgumentException("Concurrent fragment session");
        }
        if (header.payloadId() != fragment.payloadId() || header.totalBytes() != fragment.totalBytes() || header.chunks() != fragment.chunks()) {
            sessions.remove(peer);
            throw new IllegalArgumentException("Inconsistent fragment metadata");
        }
        if (active.chunks[fragment.index()] == null) active.count++;
        active.chunks[fragment.index()] = fragment.bytes();
        if (active.count < active.chunks.length) return Optional.empty();
        sessions.remove(peer);
        byte[] bytes = new byte[header.totalBytes()];
        int offset = 0;
        for (byte[] chunk : active.chunks) { System.arraycopy(chunk, 0, bytes, offset, chunk.length); offset += chunk.length; }
        if (offset != bytes.length) throw new IllegalArgumentException("Fragment byte count mismatch");
        return Optional.of(new Completed(header.payloadId(), bytes));
    }
    public void expire(long now) { sessions.values().removeIf(session -> now >= session.deadline); }
    public void remove(UUID peer) { sessions.remove(peer); }
    public void clear() { sessions.clear(); }
    public int sessionCount() { return sessions.size(); }
}
