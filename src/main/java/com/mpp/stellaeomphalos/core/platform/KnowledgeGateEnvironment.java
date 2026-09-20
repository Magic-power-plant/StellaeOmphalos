package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.function.Function;

/** Optional stage/tag integrations provide frozen data, never world-writing gate callbacks. */
public final class KnowledgeGateEnvironment {
    public record Snapshot(boolean stagesPresent, Set<String> stages, Set<String> tags) {
        public Snapshot {
            stages = Set.copyOf(stages);
            tags = Set.copyOf(tags);
        }
    }

    private static Function<ServerPlayer, Snapshot> provider =
            p -> new Snapshot(false, Set.of(), Set.of());

    private KnowledgeGateEnvironment() {}

    public static void register(Function<ServerPlayer, Snapshot> next) {
        provider = Objects.requireNonNull(next);
    }

    public static Snapshot read(ServerPlayer player) {
        return provider.apply(player);
    }
}
