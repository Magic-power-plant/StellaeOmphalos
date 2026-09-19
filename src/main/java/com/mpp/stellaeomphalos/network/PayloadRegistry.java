package com.mpp.stellaeomphalos.network;

import com.mpp.stellaeomphalos.data.codec.BoundedJson;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** One append-only protocol table, independent from physical channel framing. */
public final class PayloadRegistry {
    public enum Direction { TO_CLIENT, TO_SERVER }
    public record Type<T extends OmphalosPayload>(int id, Direction direction, Class<T> type, Codec<T> codec, int minimumMinor) {}
    private final Map<Integer, Type<?>> ids = new LinkedHashMap<>();
    private final Map<Class<?>, Type<?>> types = new LinkedHashMap<>();
    private boolean frozen;
    private int lastId = -1;
    public <T extends OmphalosPayload> void register(int id, Direction direction, Class<T> type, Codec<T> codec, int minimumMinor) {
        if (frozen || id <= lastId || types.containsKey(type)) throw new IllegalStateException("Duplicate, unordered or late payload " + id);
        var entry = new Type<>(id, direction, type, codec, minimumMinor);
        ids.put(id, entry); types.put(type, entry); lastId = id;
    }
    public void freeze() { frozen = true; }
    public Type<?> type(int id) {
        var type = ids.get(id);
        if (type == null) throw new IllegalArgumentException("Unknown payload ID " + id);
        return type;
    }
    public Type<?> type(OmphalosPayload payload) {
        var type = types.get(payload.getClass());
        if (type == null) throw new IllegalArgumentException("Unregistered payload");
        return type;
    }
    @SuppressWarnings("unchecked")
    public byte[] encode(OmphalosPayload payload) {
        var type = (Type<OmphalosPayload>) type(payload);
        var json = type.codec().encodeStart(JsonOps.INSTANCE, payload).getOrThrow(false, message -> { throw new IllegalArgumentException(message); });
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > ChunkedEnvelope.MAX_BYTES) throw new IllegalArgumentException("Payload too large");
        return bytes;
    }
    public OmphalosPayload decode(int id, Direction direction, byte[] bytes) {
        var type = type(id);
        if (type.direction() != direction || bytes.length > ChunkedEnvelope.MAX_BYTES) throw new IllegalArgumentException("Invalid payload direction or size");
        try {
            String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            return type.codec().parse(JsonOps.INSTANCE, BoundedJson.parse(new java.io.StringReader(json)))
                    .getOrThrow(false, message -> { throw new IllegalArgumentException(message); });
        } catch (java.nio.charset.CharacterCodingException exception) { throw new IllegalArgumentException("Invalid UTF-8", exception); }
    }
    public int size() { return ids.size(); }
}
