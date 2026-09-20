package com.mpp.stellaeomphalos.constellation.boon;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** Node categories of the boon tree. Ordinal order is the wire encoding for the layout payload. */
public enum BoonNodeType {
    ROOT,
    CORE_ROOT,
    KEY,
    MAJOR,
    SOCKET,
    CONNECTOR,
    NORMAL;

    public static final Codec<BoonNodeType> CODEC = Codec.STRING.xmap(BoonNodeType::byName,
            type -> type.name().toLowerCase(Locale.ROOT));

    public static BoonNodeType byName(String name) {
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown boon node type " + name);
        }
    }

    /** Roots never consume a skill point (contract: root nodes stay off the point ledger). */
    public boolean isRootLike() {
        return this == ROOT || this == CORE_ROOT;
    }
}
