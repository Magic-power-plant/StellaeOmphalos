package com.mpp.stellaeomphalos.crafting.altar.recipe;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.crafting.altar.menu.MachineMenus;
import com.mpp.stellaeomphalos.crafting.special.WorkbenchAnchorMenu;
import com.mpp.stellaeomphalos.data.registry.*;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.*;

@Mod.EventBusSubscriber(modid = Omphalos.MODID)
public final class CraftingBootstrap {
    private static MinecraftServer server;
    private static RecipeHub hub;

    static {
        RecipeCatalog.initialize();
        MachineMenus.initialize();
        com.mpp.stellaeomphalos.core.platform.WorldCraftingBridge.register(
                (level, state) ->
                        com.mpp.stellaeomphalos.crafting.transmutation.WorldRecipeIndex.of(level)
                                .melting(state));
    }

    private CraftingBootstrap() {}

    public static RecipeHub hub(MinecraftServer active) {
        if (hub == null || server != active) {
            server = active;
            var baseline =
                    active.overworld()
                            .getDataStorage()
                            .computeIfAbsent(
                                    RecipeBaseline::load,
                                    RecipeBaseline::new,
                                    "stellaeomphalos_recipe_baseline");
            hub = new RecipeHub(active.getRecipeManager(), baseline, RecipeCatalog::parse);
            hub.registerScript(WorldRecipeTables::replay);
            hub.registerScript(api -> RecipeScriptBridge.replay(active, api));
            hub.reload(active.getRecipeManager());
        }
        return hub;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(Omphalos.MODID, path);
    }

    @SubscribeEvent
    public static void start(ServerStartedEvent event) {
        hub(event.getServer());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        hub = null;
        server = null;
        com.mpp.stellaeomphalos.crafting.transmutation.WorldRecipeIndex.clear();
    }

    @SubscribeEvent
    public static void reload(OnDatapackSyncEvent event) {
        boolean alreadyInitialized = hub != null && server == event.getPlayerList().getServer();
        var recipes = hub(event.getPlayerList().getServer());
        if (event.getPlayer() == null && alreadyInitialized)
            recipes.reload(event.getPlayerList().getServer().getRecipeManager());
        var packet =
                new com.mpp.stellaeomphalos.network.toClient.PktRecipeEpoch(
                        recipes.epoch(), recipes.disabledFamilies().stream().sorted().toList());
        if (event.getPlayer() != null)
            com.mpp.stellaeomphalos.network.OmphalosChannel.send(event.getPlayer(), packet);
        else
            for (var player : event.getPlayerList().getPlayers())
                com.mpp.stellaeomphalos.network.OmphalosChannel.send(player, packet);
    }

    @SubscribeEvent
    public static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        var commands =
                net.minecraft.commands.Commands.literal(Omphalos.MODID)
                        .requires(source -> source.hasPermission(2));
        commands.then(
                net.minecraft.commands.Commands.literal("recipes")
                        .then(
                                net.minecraft.commands.Commands.literal("reset")
                                        .executes(
                                                context -> {
                                                    var recipes =
                                                            hub(context.getSource().getServer());
                                                    recipes.resetToBaseline();
                                                    var packet =
                                                            new com.mpp
                                                                    .stellaeomphalos
                                                                    .network
                                                                    .toClient
                                                                    .PktRecipeEpoch(
                                                                    recipes.epoch(),
                                                                    recipes
                                                                            .disabledFamilies()
                                                                            .stream()
                                                                            .sorted()
                                                                            .toList());
                                                    for (var player :
                                                            context.getSource()
                                                                    .getServer()
                                                                    .getPlayerList()
                                                                    .getPlayers())
                                                        com.mpp.stellaeomphalos.network
                                                                .OmphalosChannel.send(
                                                                player, packet);
                                                    context.getSource()
                                                            .sendSuccess(
                                                                    () ->
                                                                            Component.translatable(
                                                                                    "stellaeomphalos.crafting.reset"),
                                                                    true);
                                                    return 1;
                                                })));
        event.getDispatcher().register(commands);
    }

    @SubscribeEvent
    public static void workbench(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || event.getEntity().isShiftKeyDown()
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.CRAFTING_TABLE))
            return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player)
            NetworkHooks.openScreen(
                    player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new WorkbenchAnchorMenu(id, inv, event.getPos()),
                            Component.translatable("container.crafting")),
                    event.getPos());
    }
}
