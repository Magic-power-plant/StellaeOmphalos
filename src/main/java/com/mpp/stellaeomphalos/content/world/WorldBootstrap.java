package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.content.world.capability.*;
import com.mpp.stellaeomphalos.core.platform.WorldBehaviorBridge;
import com.mpp.stellaeomphalos.crafting.altar.recipe.*;
import com.mpp.stellaeomphalos.data.loader.CodecDirectoryLoader;
import com.mpp.stellaeomphalos.ritual.amplifier.AmplifierTier;
import com.mpp.stellaeomphalos.ritual.effect.RiteScheduler;
import com.mpp.stellaeomphalos.ritual.rite.*;
import com.mpp.stellaeomphalos.structure.match.*;
import com.mpp.stellaeomphalos.structure.pattern.*;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.*;

/** Content assembly owns cross-layer wiring; low-level packages retain downward dependencies. */
public final class WorldBootstrap {
    private record Loaded(ServerLevel level, ChunkPos pos, boolean fresh) {}

    private static final java.util.concurrent.ConcurrentLinkedQueue<Loaded> LOADED =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final Map<ServerLevel, RetroGenPlan> PLANS = new IdentityHashMap<>();

    private WorldBootstrap() {}

    public static void attach(IEventBus bus) {
        WorldContent.initialize();
        WorldGeneration.initialize();
        com.mpp.stellaeomphalos.content.block.DecorContent.initialize();
        com.mpp.stellaeomphalos.content.block.MachineContent.initialize();
        com.mpp.stellaeomphalos.content.item.CatalogItems.initialize();
        com.mpp.stellaeomphalos.content.menu.StationMenus.initialize();
        bus.addListener(WorldCapabilities::register);
        bus.addListener(WorldDataProvider::gather);
        bus.addListener(com.mpp.stellaeomphalos.content.block.DecorDataProvider::gather);
        var forge = MinecraftForge.EVENT_BUS;
        forge.addGenericListener(LevelChunk.class, WorldCapabilities::attach);
        forge.addListener(WorldBootstrap::reload);
        forge.addListener(WorldBootstrap::load);
        forge.addListener(WorldBootstrap::tick);
        forge.addListener(WorldBootstrap::unload);
        forge.addListener(WorldBootstrap::started);
        forge.addListener(WorldBootstrap::stop);
        forge.addListener(WorldBootstrap::commands);
        forge.addListener(WorldBootstrap::logout);
        forge.addListener(WorldBootstrap::dimension);
        StructureIntegrityHub.attach();
        WorldBehaviorBridge.register(new WorldBehaviorRegistry());
        CraftingEnvironment.registerFrameMatcher(
                (level, p, tier) ->
                        level instanceof ServerLevel server
                                && StructureIntegrityHub.of(server)
                                        .query(
                                                p,
                                                new ResourceLocation(
                                                        Omphalos.MODID,
                                                        level.getBlockState(p)
                                                                        .is(
                                                                                com.mpp
                                                                                        .stellaeomphalos
                                                                                        .content
                                                                                        .blockentity
                                                                                        .crafting
                                                                                        .CraftingContent
                                                                                        .INFUSER
                                                                                        .get())
                                                                ? "pattern_starlight_infuser"
                                                                : switch (tier) {
                                                                    case RESONANCE ->
                                                                            "pattern_altar_t2";
                                                                    case SIGN -> "pattern_altar_t3";
                                                                    default -> "pattern_altar_t4";
                                                                }))
                                        .canProduce());
    }

    private static void reload(AddReloadListenerEvent e) {
        e.addListener(new BlueprintReloadListener());
        e.addListener(
                new CodecDirectoryLoader<>(
                        "stellaeomphalos/rite", RiteRecipe.CODEC, RiteRegistry::recipes));
        e.addListener(
                new CodecDirectoryLoader<>(
                        "stellaeomphalos/amplifier_tier",
                        AmplifierTier.CODEC,
                        RiteRegistry::amplifiers));
        WorldBehaviorRegistry.reload(e);
    }

    private static void started(ServerStartedEvent e) {
        WorldProtocol.register();
    }

    private static void load(ChunkEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel level && e.getChunk() instanceof LevelChunk)
            LOADED.add(new Loaded(level, e.getChunk().getPos(), e.isNewChunk()));
    }

    private static void initialize(Loaded entry, LevelChunk chunk) {
        chunk.getCapability(WorldCapabilities.SPRING)
                .ifPresent(
                        s -> {
                            s.initialize(
                                    entry.level().getSeed(),
                                    entry.pos().x,
                                    entry.pos().z,
                                    WorldBehaviorRegistry.springs());
                            s.validateFluid();
                        });
        chunk.getCapability(WorldCapabilities.STAMP)
                .ifPresent(
                        s -> {
                            s.ready(entry.level().getSeed());
                            if (entry.fresh()) {
                                for (var phase : WorldGenPhase.values()) s.complete(phase);
                            } else if (OmphalosConfig.COMMON.snapshot().get("worldgen.retrogen")
                                            == OmphalosConfig.Retrogen.LOADED_ONLY
                                    && s.stamp() != WorldGenPhase.currentMask())
                                plan(entry.level()).enqueue(entry.pos());
                        });
        chunk.getCapability(WorldCapabilities.GEODES)
                .ifPresent(
                        index -> {
                            if (!index.dirty()) return;
                            int min = Math.max(2, chunk.getMinBuildHeight()),
                                    max = Math.min(5, chunk.getMaxBuildHeight() - 1);
                            for (int y = min; y <= max; y++)
                                for (int x = 0; x < 16; x++)
                                    for (int z = 0; z < 16; z++) {
                                        var p =
                                                new BlockPos(
                                                        chunk.getPos().getMinBlockX() + x,
                                                        y,
                                                        chunk.getPos().getMinBlockZ() + z);
                                        var oreState = chunk.getBlockState(p);
                                        // 只有 GEODE 变体进运行期晶簇索引（ASTRAL 是星辉矿）。
                                        if (oreState.is(WorldContent.GEODE_ORE.get())
                                                && !com.mpp.stellaeomphalos.content.block
                                                        .GeodeOreBlock.isAstral(oreState))
                                            index.add(p);
                                    }
                            index.clean();
                        });
    }

    public static RetroGenPlan plan(ServerLevel level) {
        return PLANS.computeIfAbsent(level, RetroGenPlan::new);
    }

    private static void tick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel level)) return;
        int budget = 16;
        for (var entry : LOADED) {
            if (budget <= 0) break;
            if (entry.level() != level) continue;
            var chunk = level.getChunkSource().getChunkNow(entry.pos().x, entry.pos().z);
            if (chunk != null) {
                LOADED.remove(entry);
                initialize(entry, chunk);
                budget--;
            }
        }
        PendingGeodeRegistration.drain(level, 256);
        WorldGeneration.drain(level);
        plan(level).tick();
        RiteScheduler.tick(level);
        com.mpp.stellaeomphalos.constellation.effect.DomainEffectSoar.tickLeases(level);
        WorldProtocol.tick(level);
        StarfallController.tick(level);
    }

    private static void unload(LevelEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel level) {
            PLANS.remove(level);
            LOADED.removeIf(l -> l.level() == level);
            RiteScheduler.unload(level);
            com.mpp.stellaeomphalos.core.util.world.BlockChangeBus.unload(level);
            com.mpp.stellaeomphalos.data.loader.DimensionArchiveRoot.unload(level);
        }
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        StarfallController.logout(e.getEntity().getUUID());
        if (e.getEntity() instanceof ServerPlayer p) {
            WorldProtocol.end(p);
            com.mpp.stellaeomphalos.constellation.effect.DomainEffectSoar.logout(p);
        }
    }

    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            WorldProtocol.end(p);
            com.mpp.stellaeomphalos.constellation.effect.DomainEffectSoar.logout(p);
        }
    }

    private static void stop(ServerStoppedEvent e) {
        LOADED.clear();
        PLANS.clear();
        WorldProtocol.clear();
        WorldGeneration.clear();
        PendingGeodeRegistration.clear();
        StarfallController.clear();
    }

    private static void commands(RegisterCommandsEvent e) {
        OmphalosCommands.register(e);
        BlueprintCommands.register(e);
        e.getDispatcher()
                .register(
                        Commands.literal(Omphalos.MODID)
                                .requires(s -> s.hasPermission(2))
                                .then(
                                        Commands.literal("revalidate")
                                                .executes(
                                                        c ->
                                                                StructureIntegrityHub.of(
                                                                                c.getSource()
                                                                                        .getLevel())
                                                                        .revalidate()))
                                .then(
                                        Commands.literal("retrogen")
                                                .then(
                                                        Commands.argument(
                                                                        "radius",
                                                                        com.mojang.brigadier
                                                                                .arguments
                                                                                .IntegerArgumentType
                                                                                .integer(0, 8))
                                                                .executes(
                                                                        c -> {
                                                                            var p =
                                                                                    c.getSource()
                                                                                            .getPlayerOrException();
                                                                            int r =
                                                                                    com.mojang
                                                                                            .brigadier
                                                                                            .arguments
                                                                                            .IntegerArgumentType
                                                                                            .getInteger(
                                                                                                    c,
                                                                                                    "radius");
                                                                            int count = 0;
                                                                            for (int x = -r;
                                                                                    x <= r;
                                                                                    x++)
                                                                                for (int z = -r;
                                                                                        z <= r;
                                                                                        z++) {
                                                                                    var chunk =
                                                                                            p.serverLevel()
                                                                                                    .getChunkSource()
                                                                                                    .getChunkNow(
                                                                                                            (p.blockPosition()
                                                                                                                                    .getX()
                                                                                                                            >> 4)
                                                                                                                    + x,
                                                                                                            (p.blockPosition()
                                                                                                                                    .getZ()
                                                                                                                            >> 4)
                                                                                                                    + z);
                                                                                    if (chunk
                                                                                            != null) {
                                                                                        plan(p
                                                                                                        .serverLevel())
                                                                                                .enqueue(
                                                                                                        chunk
                                                                                                                .getPos());
                                                                                        count++;
                                                                                    }
                                                                                }
                                                                            WorldProtocol
                                                                                    .watchRetrogen(
                                                                                            p);
                                                                            return count;
                                                                        })))
                                .then(
                                        Commands.literal("preview")
                                                .then(
                                                        Commands.argument(
                                                                        "blueprint",
                                                                        net.minecraft.commands
                                                                                .arguments
                                                                                .ResourceLocationArgument
                                                                                .id())
                                                                .executes(
                                                                        c -> {
                                                                            var p =
                                                                                    c.getSource()
                                                                                            .getPlayerOrException();
                                                                            return WorldProtocol
                                                                                            .preview(
                                                                                                    p,
                                                                                                    net
                                                                                                            .minecraft
                                                                                                            .commands
                                                                                                            .arguments
                                                                                                            .ResourceLocationArgument
                                                                                                            .getId(
                                                                                                                    c,
                                                                                                                    "blueprint"),
                                                                                                    p
                                                                                                            .blockPosition())
                                                                                    ? 1
                                                                                    : 0;
                                                                        }))));
    }
}
