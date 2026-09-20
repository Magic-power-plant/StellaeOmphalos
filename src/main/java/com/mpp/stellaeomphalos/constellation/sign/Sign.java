package com.mpp.stellaeomphalos.constellation.sign;

import com.mpp.stellaeomphalos.constellation.starmap.StarLine;
import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A constellation: identity, drawing geometry, palette and discovery rule. */
public interface Sign {
    ResourceLocation id();
    Component displayName();
    int renderColor();
    List<StarPoint> stars();
    List<StarLine> lines();
    List<ItemStack> signatureItems();
    /** Whether the given player may discover this sign. */
    boolean canDiscover(ServerPlayer player, SignDiscoveryView progress);
}
