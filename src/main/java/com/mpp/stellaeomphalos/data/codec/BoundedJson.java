package com.mpp.stellaeomphalos.data.codec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;

/** Strict JSON reader with bounded nesting and allocation, shared by packets and data tables. */
public final class BoundedJson {
    private static final int MAX_DEPTH = 32;
    private static final int MAX_NODES = 131072;
    private BoundedJson() {}
    public static JsonElement parse(Reader source) {
        try {
            var bounded = new java.io.FilterReader(source) {
                private int consumed;
                @Override public int read(char[] buffer, int offset, int length) throws IOException {
                    int count = super.read(buffer, offset, length);
                    if (count > 0 && (consumed += count) > 4 * 1024 * 1024) throw new IOException("JSON exceeds 4 MiB character limit");
                    return count;
                }
                @Override public int read() throws IOException {
                    int value = super.read();
                    if (value >= 0 && ++consumed > 4 * 1024 * 1024) throw new IOException("JSON exceeds character limit");
                    return value;
                }
            };
            var reader = new JsonReader(bounded); reader.setLenient(false);
            var result = value(reader, 0, new int[]{0});
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Trailing JSON content at " + reader.getPath());
            return result;
        } catch (IOException exception) { throw new IllegalArgumentException(exception.getMessage(), exception); }
    }
    private static JsonElement value(JsonReader reader, int depth, int[] nodes) throws IOException {
        if (depth > MAX_DEPTH || ++nodes[0] > MAX_NODES) throw new IllegalArgumentException("JSON complexity limit at " + reader.getPath());
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject(); var object = new JsonObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw new IllegalArgumentException("Duplicate field at " + reader.getPath());
                    object.add(name, value(reader, depth + 1, nodes));
                }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                reader.beginArray(); var array = new JsonArray();
                while (reader.hasNext()) array.add(value(reader, depth + 1, nodes));
                reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException("Expected JSON value at " + reader.getPath());
        };
    }
}
