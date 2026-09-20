package com.mpp.stellaeomphalos.knowledge.codex;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record CodexRoute(ResourceLocation node, int page) {
    public CodexRoute {
        if (page < 0 || page > 4095) throw new IllegalArgumentException("Invalid codex page");
    }

    public String encode() {
        return node + "#" + page;
    }

    public static Optional<CodexRoute> parse(String value) {
        try {
            String[] parts = value.split("#", -1);
            if (parts.length != 2 || parts[0].isBlank() || value.length() > 512)
                return Optional.empty();
            return Optional.of(
                    new CodexRoute(new ResourceLocation(parts[0]), Integer.parseInt(parts[1])));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
