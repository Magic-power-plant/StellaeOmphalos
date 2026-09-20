package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

/** Domain capability shared by crafting, knowledge and player services. */
public interface KnowledgeAccess {
    StarRecordView view(ServerPlayer player);

    boolean promote(ServerPlayer player, StarTier tier);

    boolean unlockBranch(ServerPlayer player, String branch);

    boolean unlockNode(ServerPlayer player, ResourceLocation node);

    boolean collectShard(ServerPlayer player, ResourceLocation shard);

    boolean recordTarget(ServerPlayer player, ResourceLocation target);

    boolean readPage(ServerPlayer player, ResourceLocation page, String route);
}
