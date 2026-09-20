package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class CraftCompletedEvent extends net.minecraftforge.eventbus.api.Event {
    private final ServerLevel level;
    private final BlockPos position;
    private final UUID crafter;
    private final ResourceLocation recipe;
    private final ItemStack output;
    private final float experience;

    public CraftCompletedEvent(
            ServerLevel level,
            BlockPos position,
            UUID crafter,
            ResourceLocation recipe,
            ItemStack output,
            float experience) {
        this.level = level;
        this.position = position.immutable();
        this.crafter = crafter;
        this.recipe = recipe;
        this.output = output.copy();
        this.experience = experience;
    }

    public ServerLevel level() {
        return level;
    }

    public BlockPos position() {
        return position;
    }

    public UUID crafter() {
        return crafter;
    }

    public ResourceLocation recipe() {
        return recipe;
    }

    public ItemStack output() {
        return output.copy();
    }

    public float experience() {
        return experience;
    }
}
