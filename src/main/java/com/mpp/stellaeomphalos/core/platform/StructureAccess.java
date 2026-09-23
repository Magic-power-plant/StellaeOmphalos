package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** 《星典知识与玩家进度》 can install knowledge queries without an upward structure -> player dependency. */
public final class StructureAccess {
    public interface Progress {
        boolean canPreview(ServerPlayer player, ResourceLocation blueprint);

        int astrolabePrecision(ServerPlayer player, ResourceLocation target);
    }

    private static Progress progress =
            new Progress() {
                public boolean canPreview(ServerPlayer p, ResourceLocation id) {
                    return p.isCreative()
                            || p.getPersistentData()
                                    .getCompound("UnlockedStructures")
                                    .getBoolean(id.toString());
                }

                public int astrolabePrecision(ServerPlayer p, ResourceLocation id) {
                    return p.isCreative() ? 2 : 0;
                }
            };

    private StructureAccess() {}

    public static void register(Progress value) {
        progress = Objects.requireNonNull(value);
    }

    public static Progress progress() {
        return progress;
    }
}
