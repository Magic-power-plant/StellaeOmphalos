package com.mpp.stellaeomphalos.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ChunkedEnvelope(UUID session, int chunks, int index, int payloadId, int totalBytes, byte[] bytes) {
    public static final int THRESHOLD = 16 * 1024;
    public static final int CHUNK_SIZE = 12 * 1024;
    public static final int MAX_BYTES = 512 * 1024;
    public static final int MAX_CHUNKS = 48;
    public ChunkedEnvelope {
        java.util.Objects.requireNonNull(session);
        bytes = bytes.clone();
        if (payloadId < 0 || totalBytes < 1 || totalBytes > MAX_BYTES || chunks < 1 || chunks > MAX_CHUNKS
                || chunks != (totalBytes + CHUNK_SIZE - 1) / CHUNK_SIZE || index < 0 || index >= chunks
                || bytes.length != Math.min(CHUNK_SIZE, totalBytes - index * CHUNK_SIZE))
            throw new IllegalArgumentException("Invalid fragment header or length");
    }
    @Override public byte[] bytes() { return bytes.clone(); }
    public static List<ChunkedEnvelope> split(int payloadId, byte[] data) {
        if (data.length < 1 || data.length > MAX_BYTES) throw new IllegalArgumentException("Invalid payload size");
        var id = UUID.randomUUID();
        int count = (data.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        var result = new ArrayList<ChunkedEnvelope>(count);
        for (int i = 0; i < count; i++) result.add(new ChunkedEnvelope(id, count, i, payloadId, data.length,
                java.util.Arrays.copyOfRange(data, i * CHUNK_SIZE, Math.min(data.length, (i + 1) * CHUNK_SIZE))));
        return List.copyOf(result);
    }
}
