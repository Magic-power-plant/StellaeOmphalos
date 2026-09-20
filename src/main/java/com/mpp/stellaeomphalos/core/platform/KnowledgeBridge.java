package com.mpp.stellaeomphalos.core.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.*;

/** Content assembly installs these capabilities; lower domains never import player or knowledge. */
public final class KnowledgeBridge {
    private static KnowledgeAccess access;
    private static Consumer<Player> opener = player -> {};
    private static BiPredicate<ServerPlayer, ResourceLocation> recipes = (player, id) -> true;

    private KnowledgeBridge() {}

    public static void install(KnowledgeAccess value) {
        access = Objects.requireNonNull(value);
    }

    public static KnowledgeAccess access() {
        if (access == null) throw new IllegalStateException("Knowledge service not installed");
        return access;
    }

    public static void installOpener(Consumer<Player> value) {
        opener = Objects.requireNonNull(value);
    }

    public static void open(Player player) {
        opener.accept(player);
    }

    public static void recipeGate(BiPredicate<ServerPlayer, ResourceLocation> value) {
        recipes = Objects.requireNonNull(value);
    }

    public static boolean canCraft(ServerPlayer player, ResourceLocation id) {
        return recipes.test(player, id);
    }
}
