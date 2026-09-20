package com.mpp.stellaeomphalos.content.item.knowledge;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.content.item.*;
import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.data.loader.CodecDirectoryLoader;
import com.mpp.stellaeomphalos.knowledge.advancement.ModMilestones;
import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.knowledge.research.*;
import com.mpp.stellaeomphalos.network.toClient.*;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import com.mpp.stellaeomphalos.player.mantle.MantleDispatcher;
import com.mpp.stellaeomphalos.player.progress.*;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.*;

/**
 * Cross-domain assembly lives in content. knowledge/player only exchange L0 capabilities/events.
 */
public final class KnowledgeBootstrap {
    private KnowledgeBootstrap() {}

    public static void attach(IEventBus bus) {
        KnowledgeCatalog.initialize();
        KnowledgeContent.initialize();
        ModMilestones.initialize();
        KnowledgeBridge.install(StarRecords.ACCESS);
        MantleDispatcher.attach();
        KnowledgeBridge.recipeGate(
                (p, recipe) ->
                        p.isCreative()
                                || StarRecords.get(p)
                                        .tier()
                                        .reaches(KnowledgeCatalog.recipeTier(recipe)));
        StructureAccess.register(
                new StructureAccess.Progress() {
                    public boolean canPreview(
                            ServerPlayer p, net.minecraft.resources.ResourceLocation blueprint) {
                        return p.isCreative()
                                || KnowledgeCatalog.NODES.all().stream()
                                        .filter(
                                                n ->
                                                        n.visibility(KnowledgeProtocol.context(p))
                                                                .level()
                                                                .readable())
                                        .flatMap(n -> n.pages().stream())
                                        .map(KnowledgeCatalog.PAGES::find)
                                        .flatMap(Optional::stream)
                                        .anyMatch(
                                                page ->
                                                        page.kind() == PageKind.STRUCTURE
                                                                && page.reference()
                                                                        .equals(
                                                                                blueprint
                                                                                        .toString()));
                    }

                    public int astrolabePrecision(
                            ServerPlayer p, net.minecraft.resources.ResourceLocation target) {
                        return p.isCreative()
                                ? 2
                                : StarRecords.get(p).tier().reaches(StarTier.RADIANCE)
                                        ? 2
                                        : StarRecords.get(p).tier().reaches(StarTier.ATTUNEMENT)
                                                ? 1
                                                : 0;
                    }
                });
        bus.addListener(KnowledgeDataProvider::gather);
        var forge = MinecraftForge.EVENT_BUS;
        forge.addListener(KnowledgeBootstrap::started);
        forge.addListener(KnowledgeBootstrap::stop);
        forge.addListener(KnowledgeBootstrap::login);
        forge.addListener(KnowledgeBootstrap::logout);
        forge.addListener(KnowledgeBootstrap::tick);
        forge.addListener(KnowledgeBootstrap::reload);
        forge.addListener(KnowledgeBootstrap::crafted);
        forge.addListener(KnowledgeBootstrap::vanillaCrafted);
        forge.addListener(KnowledgeBootstrap::milestone);
        forge.addListener(KnowledgeBootstrap::commands);
        forge.addListener(KnowledgeBootstrap::respawn);
        forge.addListener(KnowledgeBootstrap::dimension);
    }

    private static void started(ServerStartedEvent e) {
        KnowledgeProtocol.clear();
        KnowledgeProtocol.register();
        StarRecords.store(e.getServer());
    }

    private static void stop(ServerStoppingEvent e) {
        StarRecords.stop(e.getServer());
        KnowledgeProtocol.clear();
        MantleDispatcher.clear();
    }

    private static void reload(AddReloadListenerEvent e) {
        e.addListener(
                new CodecDirectoryLoader<>(
                        "codex_page", CodexPage.CODEC, KnowledgeCatalog.PAGES::reload));
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        var record = StarRecords.get(p);
        BoonProgress.getServer(p).persist(p);
        if (record.reward()) {
            if (OmphalosConfig.SERVER.flag("gameplay.giveCodexOnFirstJoin")) {
                var book = new ItemStack(KnowledgeContent.CODEX.get());
                if (!p.addItem(book)) p.drop(book, false);
            }
            StarRecords.dirty(p);
        }
        StarRecords.notifyLogin(p);
        KnowledgeProtocol.synchronize(p);
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        StarRecords.dirty(p);
        save(p.server);
        KnowledgeProtocol.logout(p.getUUID());
        MantleDispatcher.logout(p.getUUID());
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        resync(e);
    }

    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        resync(e);
    }

    private static void resync(PlayerEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer)) {
            KnowledgeProtocol.logout(p.getUUID());
            KnowledgeProtocol.synchronize(p);
        }
    }

    private static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        int tick = e.getServer().getTickCount();
        for (var p : e.getServer().getPlayerList().getPlayers()) {
            if (tick % 10 == 0) {
                KnowledgeProtocol.synchronize(p);
                KnowledgeProtocol.projections(p);
            }
        }
        if (tick % (20 * OmphalosConfig.SERVER.integer("progression.saveIntervalSeconds")) == 0)
            save(e.getServer());
    }

    public static void save(net.minecraft.server.MinecraftServer server) {
        StarRecords.store(server)
                .save(
                        server.getWorldPath(LevelResource.ROOT)
                                .resolve("data")
                                .resolve(StarRecordStore.NAME + ".dat")
                                .toFile());
    }

    public static void promote(ServerPlayer player, StarTier tier) {
        if (player instanceof FakePlayer) return;
        boolean changed = StarRecords.ACCESS.promote(player, tier);
        var unlocked = new ArrayList<String>();
        for (var branch : StudyBranch.values())
            if (tier.reaches(branch.requiredTier())) {
                if (StarRecords.ACCESS.unlockBranch(player, branch.name()))
                    unlocked.add(branch.name());
            }
        if (!unlocked.isEmpty())
            com.mpp.stellaeomphalos.network.SafeDispatch.send(player, new PktCodexUnlock(unlocked));
        if (changed) {
            StarRecords.dirty(player);
            save(player.server);
        }
    }

    private static void crafted(CraftCompletedEvent e) {
        if (e.crafter() == null) return;
        var p = e.level().getServer().getPlayerList().getPlayer(e.crafter());
        if (p == null || p instanceof FakePlayer) return;
        var recipe = e.level().getRecipeManager().byKey(e.recipe()).orElse(null);
        StarTier tier = KnowledgeCatalog.recipeTier(e.recipe());
        if (recipe instanceof AsterismUpgradeRecipe upgrade)
            tier =
                    switch (upgrade.target()) {
                        case RESONANCE -> StarTier.ATTUNEMENT;
                        case SIGN -> StarTier.CONSTELLATION;
                        case TRAIT, RADIANCE -> StarTier.RADIANCE;
                        default -> StarTier.BASIC_CRAFT;
                    };
        promote(p, tier);
        if (recipe instanceof AsterismRecipe altar) {
            ModMilestones.find("altar")
                    .ifPresent(trigger -> trigger.fire(p, e.recipe(), "", 1, altar.tier().name()));
        }
    }

    private static void vanillaCrafted(PlayerEvent.ItemCraftedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer) return;
        var id =
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                        e.getCrafting().getItem());
        if (id.getNamespace().equals(Omphalos.MODID)
                && id.getPath().equals("asterism_altar_discovery"))
            promote(p, StarTier.BASIC_CRAFT);
    }

    private static void milestone(ProgressMilestoneEvent e) {
        if (e.player() instanceof FakePlayer) return;
        if (e.kind().equals("resonance")) {
            StarRecords.mutate(e.player(), StarRecord::resonance);
            promote(e.player(), StarTier.ATTUNEMENT);
        }
        if (e.kind().equals("shard")) {
            ModMilestones.find("shard")
                    .ifPresent(
                            trigger ->
                                    trigger.fireMatching(
                                            e.player(),
                                            criteria -> {
                                                long count =
                                                        StarRecords.get(e.player())
                                                                .unlockedShards()
                                                                .stream()
                                                                .map(KnowledgeCatalog.SHARDS::find)
                                                                .flatMap(Optional::stream)
                                                                .filter(
                                                                        shard ->
                                                                                criteria.names()
                                                                                                .isEmpty()
                                                                                        || !shard.boundSign()
                                                                                                        .isBlank()
                                                                                                && criteria.names()
                                                                                                        .contains(
                                                                                                                new net
                                                                                                                        .minecraft
                                                                                                                        .resources
                                                                                                                        .ResourceLocation(
                                                                                                                        shard
                                                                                                                                .boundSign())))
                                                                .count();
                                                return count >= criteria.minimum();
                                            }));
            return;
        }
        if (e.kind().equals("boon")
                && e.amount() >= 10
                && StarRecords.get(e.player()).tier().reaches(StarTier.RADIANCE))
            promote(e.player(), StarTier.BRILLIANCE);
        String category = "";
        var sign = com.mpp.stellaeomphalos.constellation.sign.SignRegistry.byId(e.subject());
        if (sign != null)
            category =
                    sign instanceof com.mpp.stellaeomphalos.constellation.sign.MajorSign
                            ? "major"
                            : sign instanceof com.mpp.stellaeomphalos.constellation.sign.RitualSign
                                    ? "weak"
                                    : "minor";
        final String actual = category;
        ModMilestones.find(e.kind())
                .ifPresent(
                        trigger ->
                                trigger.fire(
                                        e.player(),
                                        e.subject(),
                                        actual,
                                        e.kind().equals("resonance")
                                                ? StarRecords.get(e.player()).resonanceCount()
                                                : e.amount(),
                                        StarRecords.get(e.player()).tier().name()));
    }

    private static void commands(RegisterCommandsEvent e) {
        for (String root : List.of(Omphalos.MODID, "stellae")) {
            var codex =
                    Commands.literal("codex")
                            .then(
                                    Commands.literal("open")
                                            .executes(
                                                    c -> {
                                                        var p =
                                                                c.getSource()
                                                                        .getPlayerOrException();
                                                        com.mpp.stellaeomphalos.network.SafeDispatch
                                                                .send(
                                                                        p,
                                                                        new PktOpenCodex(
                                                                                StarRecords.get(p)
                                                                                        .lastRoute()));
                                                        return 1;
                                                    }))
                            .then(
                                    Commands.literal("dump")
                                            .executes(
                                                    c -> {
                                                        var p =
                                                                c.getSource()
                                                                        .getPlayerOrException();
                                                        var context = KnowledgeProtocol.context(p);
                                                        for (var node :
                                                                KnowledgeCatalog.NODES.all())
                                                            c.getSource()
                                                                    .sendSuccess(
                                                                            () ->
                                                                                    Component
                                                                                            .literal(
                                                                                                    node
                                                                                                                    .id()
                                                                                                            + "="
                                                                                                            + node.visibility(
                                                                                                                            context)
                                                                                                                    .level()),
                                                                            false);
                                                        return KnowledgeCatalog.NODES.all().size();
                                                    }));
            var record =
                    Commands.literal("record")
                            .requires(c -> c.hasPermission(2))
                            .then(
                                    Commands.literal("save")
                                            .executes(
                                                    c -> {
                                                        save(c.getSource().getServer());
                                                        return 1;
                                                    }))
                            .then(
                                    Commands.literal("tier")
                                            .then(
                                                    Commands.argument(
                                                                    "tier",
                                                                    com.mojang.brigadier.arguments
                                                                            .StringArgumentType
                                                                            .word())
                                                            .executes(
                                                                    c -> {
                                                                        var tier =
                                                                                StarTier.valueOf(
                                                                                        com.mojang
                                                                                                .brigadier
                                                                                                .arguments
                                                                                                .StringArgumentType
                                                                                                .getString(
                                                                                                        c,
                                                                                                        "tier")
                                                                                                .toUpperCase(
                                                                                                        Locale
                                                                                                                .ROOT));
                                                                        promote(
                                                                                c.getSource()
                                                                                        .getPlayerOrException(),
                                                                                tier);
                                                                        return 1;
                                                                    })));
            e.getDispatcher().register(Commands.literal(root).then(codex).then(record));
        }
    }
}
