package com.mpp.stellaeomphalos.data.loader;

import java.util.Objects;

/** Stable key and schema declaration for one dimension archive. */
public record ArchiveKey(String id, int schemaVersion) {
    public ArchiveKey {
        if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid archive id");
        if (schemaVersion < 1) throw new IllegalArgumentException("Invalid archive schema");
    }

    public String storageName() { return "stellaeomphalos_" + id; }
}
