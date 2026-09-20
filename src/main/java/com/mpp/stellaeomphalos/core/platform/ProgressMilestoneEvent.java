package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** An authoritative completed action, never a client request. */
public final class ProgressMilestoneEvent extends net.minecraftforge.eventbus.api.Event {
    private final ServerPlayer player;
    private final String kind;
    private final ResourceLocation subject;
    private final int amount;

    public ProgressMilestoneEvent(
            ServerPlayer player, String kind, ResourceLocation subject, int amount) {
        this.player = player;
        this.kind = kind;
        this.subject = subject;
        this.amount = amount;
    }

    public ServerPlayer player() {
        return player;
    }

    public String kind() {
        return kind;
    }

    public ResourceLocation subject() {
        return subject;
    }

    public int amount() {
        return amount;
    }
}
