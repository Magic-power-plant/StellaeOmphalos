package com.mpp.stellaeomphalos.network;

public record ProtocolVersion(int major, int minor) {
    public static final ProtocolVersion CURRENT = new ProtocolVersion(1, 3);

    public ProtocolVersion {
        if (major < 0 || minor < 0) throw new IllegalArgumentException("Negative protocol version");
    }

    public static ProtocolVersion parse(String value) {
        if (!value.matches("[0-9]+\\.[0-9]+"))
            throw new IllegalArgumentException("Invalid protocol version");
        var parts = value.split("\\.");
        return new ProtocolVersion(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public boolean accepts(String remote) {
        try {
            return parse(remote).major == major;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
