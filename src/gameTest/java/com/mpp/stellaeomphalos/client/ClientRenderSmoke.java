package com.mpp.stellaeomphalos.client;

import com.mojang.logging.LogUtils;
import com.mpp.stellaeomphalos.client.event.ClientRenderEvents;
import com.mpp.stellaeomphalos.client.particle.ParticleSpawner;
import com.mpp.stellaeomphalos.client.render.DeferredEffectQueue;
import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes;
import com.mpp.stellaeomphalos.client.screen.*;
import com.mpp.stellaeomphalos.content.block.MachineContent;
import com.mpp.stellaeomphalos.content.blockentity.crafting.CraftingContent;
import com.mpp.stellaeomphalos.content.blockentity.lumen.LumenContent;
import com.mpp.stellaeomphalos.content.world.WorldContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.concurrent.CompletableFuture;

/** Creates its own disposable flat world; never opens or edits an existing player save. */
public final class ClientRenderSmoke {
    private static int ticks, stage, renderedFrames, maxVertices, maxMachines;
    private static long started;
    private static int meshCloses,reloads;
    private static CompletableFuture<Void> reload;
    private static Object originalQuality, originalBudget, originalFallback;
    private static boolean settingsRestored;
    private static int originalRenderDistance, originalSimulationDistance;

    private static void restoreSettings() {
        if (settingsRestored) return;
        settingsRestored = true;
        var config = com.mpp.stellaeomphalos.OmphalosConfig.CLIENT;
        config.set("particles.quality", originalQuality.toString());
        config.set("particles.budget", originalBudget.toString());
        config.set("render.vanillaShaders", originalFallback.toString());
        var mc = Minecraft.getInstance();
        mc.options.renderDistance().set(originalRenderDistance);
        mc.options.simulationDistance().set(originalSimulationDistance);
    }

    private static final BlockPos ORIGIN = new BlockPos(0, -59, 0);

    private ClientRenderSmoke() {}

    public static void tick(Minecraft mc) {
        if (started == 0) started = System.currentTimeMillis();
        if (System.currentTimeMillis() - started > 240000)
            throw new AssertionError("Part 7 smoke timed out at stage " + stage);
        if (mc.getOverlay() != null) return;
        if (stage == 0 && mc.screen instanceof TitleScreen) {
            stage = 1;
            originalRenderDistance = mc.options.renderDistance().get();
            originalSimulationDistance = mc.options.simulationDistance().get();
            mc.options.renderDistance().set(4);
            mc.options.simulationDistance().set(5);
            var settings =
                    new LevelSettings(
                            "Part 7 smoke",
                            GameType.CREATIVE,
                            false,
                            Difficulty.PEACEFUL,
                            true,
                            new GameRules(),
                            WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows()
                    .createFreshLevel(
                            "part7-smoke-" + System.currentTimeMillis(),
                            settings,
                            new WorldOptions(701L, false, false),
                            registries ->
                                    registries
                                            .registryOrThrow(Registries.WORLD_PRESET)
                                            .getHolderOrThrow(WorldPresets.FLAT)
                                            .value()
                                            .createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (stage == 1) {
            stage = 2;
            mc.getSingleplayerServer()
                    .execute(
                            () -> {
                                var server = mc.getSingleplayerServer();
                                var level = server.overworld();
                                level.setDayTime(18000);
                                level.setWeatherParameters(6000, 0, false, false);
                                level.getGameRules()
                                        .getRule(GameRules.RULE_DAYLIGHT)
                                        .set(false, server);
                                for (int x = -8; x <= 8; x++)
                                    for (int z = -8; z <= 8; z++)
                                        level.setBlock(
                                                ORIGIN.offset(x, -1, z),
                                                Blocks.QUARTZ_BLOCK.defaultBlockState(),
                                                3);
                                var states =
                                        java.util.List.of(
                                                LumenContent.COLLECTOR_BLOCK
                                                        .get()
                                                        .defaultBlockState(),
                                                CraftingContent.GRINDWHEEL
                                                        .get()
                                                        .defaultBlockState(),
                                                MachineContent.STAR_CHART_TABLE
                                                        .get()
                                                        .defaultBlockState(),
                                                MachineContent.RESONANCE_ALTAR
                                                        .get()
                                                        .defaultBlockState(),
                                                MachineContent.BEAM_LENS.get().defaultBlockState(),
                                                MachineContent.BEAM_PRISM.get().defaultBlockState(),
                                                WorldContent.BLOCKS
                                                        .get("observatory")
                                                        .get()
                                                        .defaultBlockState(),
                                                MachineContent.SPYGLASS.get().defaultBlockState(),
                                                WorldContent.BLOCKS
                                                        .get("gate_core")
                                                        .get()
                                                        .defaultBlockState());
                                for (int i = 0; i < states.size(); i++)
                                    level.setBlock(
                                            ORIGIN.offset((i % 3 - 1) * 3, 0, (i / 3 - 1) * 3),
                                            states.get(i),
                                            3);
                                for (var player : server.getPlayerList().getPlayers()) {
                                    player.teleportTo(level, 2, -57, 10, 170, 20);
                                    player.setItemInHand(
                                            net.minecraft.world.InteractionHand.MAIN_HAND,
                                            new net.minecraft.world.item.ItemStack(
                                                    com.mpp.stellaeomphalos.content.item
                                                            .CatalogItems.HAND_SPYGLASS
                                                            .get()));
                                    player.setItemSlot(
                                            net.minecraft.world.entity.EquipmentSlot.CHEST,
                                            new net.minecraft.world.item.ItemStack(
                                                    com.mpp.stellaeomphalos.content.item
                                                            .CatalogItems.MANTLE
                                                            .get()));
                                    player.getAbilities().flying = true;
                                    player.onUpdateAbilities();
                                }
                            });
            mc.setScreen(null);
        }
        ticks++;
        maxVertices = Math.max(maxVertices, ClientRenderEvents.worldVertices());
        maxMachines = Math.max(maxMachines, DeferredEffectQueue.lastFlushed());
        if (ticks == 60) {
            var previewId=new net.minecraft.resources.ResourceLocation("stellaeomphalos:part7_smoke");
            var previewPos=ORIGIN.offset(-5,1,0);
            OmphalosClient.handlers().dispatch(mc,new com.mpp.stellaeomphalos.network.toClient.PreviewStartPayload(previewId,previewPos,0,0,600,7));
            OmphalosClient.handlers().dispatch(mc,new com.mpp.stellaeomphalos.network.toClient.PreviewDiffPayload(previewId,previewPos,
                    java.util.List.of(BlockPos.ZERO.asLong(),new BlockPos(1,0,0).asLong()),
                    java.util.List.of(net.minecraft.world.level.block.Block.getId(Blocks.STONE_BRICKS.defaultBlockState()),net.minecraft.world.level.block.Block.getId(Blocks.OAK_PLANKS.defaultBlockState())),2,7,true));
            var keep=new java.util.concurrent.atomic.AtomicBoolean(true);
            var loop=new com.mpp.stellaeomphalos.client.sound.LoopingMachineSound(
                    net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new net.minecraft.resources.ResourceLocation("stellaeomphalos:altar_craft_loop")),ORIGIN,keep::get);
            loop.tick();if(loop.isStopped())throw new AssertionError("Loop stopped while active");
            keep.set(false);loop.tick();if(!loop.isStopped())throw new AssertionError("Loop did not stop within one tick");

            var config = com.mpp.stellaeomphalos.OmphalosConfig.CLIENT;
            originalQuality = config.snapshot().get("particles.quality");
            originalBudget = config.snapshot().get("particles.budget");
            originalFallback = config.snapshot().get("render.vanillaShaders");
            var particle =
                    com.mpp.stellaeomphalos.content.particle.ClientVisualContent.PARTICLES
                            .get("lumen_spark")
                            .get();
            double px = mc.player.getX(), py = mc.player.getY() + 1, pz = mc.player.getZ() - 2;
            try {
                config.set("particles.quality", "OFF");
                if (ParticleSpawner.spawn(particle, px, py, pz, 0, 0, 0) != null)
                    throw new AssertionError("OFF admitted a particle");
                config.set("particles.quality", "NORMAL");
                config.set("particles.budget", "1");
                ParticleSpawner.clear();
                var one = ParticleSpawner.spawn(particle, px, py, pz, 0, 0, 0);
                if (one == null || ParticleSpawner.spawn(particle, px, py, pz, 0, 0, 0) != null)
                    throw new AssertionError("Particle budget gate failed");
                one.remove();
                if (ParticleSpawner.alive() != 0)
                    throw new AssertionError("Particle lease did not release");
                config.set("particles.budget", "2000");
                if (ParticleSpawner.spawn(
                                new com.mpp.stellaeomphalos.content.particle.ResonanceRingOptions(
                                        1, 0xffaaddff),
                                px,
                                py,
                                pz,
                                0,
                                0,
                                0)
                        == null) throw new AssertionError("Ring provider missing");
            } finally {
                config.set("particles.quality", originalQuality.toString());
                config.set("particles.budget", originalBudget.toString());
            }

            try (var stream =
                            mc.getResourceManager()
                                    .open(
                                            new net.minecraft.resources.ResourceLocation(
                                                    "stellaeomphalos:sounds.json"));
                    var reader =
                            new java.io.InputStreamReader(
                                    stream, java.nio.charset.StandardCharsets.UTF_8)) {
                var catalog = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                var ids = com.mpp.stellaeomphalos.core.registry.ModSounds.ENTRIES.ids();
                if (catalog.size() != ids.size())
                    throw new AssertionError("Sound catalog/registry mismatch");
                for (var id : ids)
                    if (!catalog.has(id.getPath()))
                        throw new AssertionError("Sound catalog missing " + id);
            } catch (java.io.IOException failure) {
                throw new AssertionError(failure);
            }
            var missing = mc.getModelManager().getMissingModel();
            for (var id : com.mpp.stellaeomphalos.core.registry.ModBlocks.ENTRIES.ids()) {
                var block = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);
                for (var state : block.getStateDefinition().getPossibleStates())
                    if (mc.getBlockRenderer().getBlockModel(state) == missing)
                        throw new AssertionError("Missing blockstate model " + state);
            }
            for (var id : com.mpp.stellaeomphalos.core.registry.ModItems.ENTRIES.ids())
                if (mc.getItemRenderer()
                                .getModel(
                                        new net.minecraft.world.item.ItemStack(
                                                net.minecraftforge.registries.ForgeRegistries.ITEMS
                                                        .getValue(id)),
                                        mc.level,
                                        mc.player,
                                        0)
                        == missing) throw new AssertionError("Missing item model " + id);
            if (!mc.getItemRenderer()
                    .getModel(
                            new net.minecraft.world.item.ItemStack(
                                    com.mpp.stellaeomphalos.content.item.CatalogItems.HAND_SPYGLASS
                                            .get()),
                            mc.level,
                            mc.player,
                            0)
                    .isCustomRenderer())
                throw new AssertionError("Hand telescope code model not installed");
            if (net.minecraftforge.client.extensions.common.IClientItemExtensions.of(
                                    com.mpp.stellaeomphalos.content.item.CatalogItems.HAND_SPYGLASS
                                            .get())
                            .getCustomRenderer()
                    == mc.getItemRenderer().getBlockEntityRenderer())
                throw new AssertionError("Hand telescope extension missing");
            if (OmphalosRenderTypes.loadedShaderCount() != 5)
                throw new AssertionError("Five custom shaders must be live");
            for (var type :
                    com.mpp.stellaeomphalos.content.particle.ClientVisualContent.PARTICLES.values())
                if (ParticleSpawner.spawn(
                                type.get(),
                                mc.player.getX(),
                                mc.player.getY() + 1,
                                mc.player.getZ() - 2,
                                0,
                                .01,
                                0)
                        == null) throw new AssertionError("Particle provider missing " + type.id());
            ClientRenderEvents.EFFECTS.spawn(
                    new com.mpp.stellaeomphalos.client.effect.ArcTrack(
                            19, -3, -57, 0, 3, -56, 0, 0xffaacfff));
        }
        if(ticks==70&&ClientRenderEvents.ghostVertices()==0)throw new AssertionError("Mandatory ghost preview emitted no vertices");
        if (ticks == 75)
            com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.set("render.vanillaShaders", "true");
        if (ticks == 85) {
            if (ClientRenderEvents.worldVertices() == 0)
                throw new AssertionError("Vanilla fallback lost geometry");
            screenshot(mc, "part7-vanilla-fallback.png");
            com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.set(
                    "render.vanillaShaders", originalFallback.toString());
        }
        if (ticks == 87)
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        if (ticks == 89) screenshot(mc, "part7-mantle.png");
        if (ticks == 90) {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            screenshot(mc, "part7-world.png");
        }
        if (ticks == 92) {
            var player = mc.player;
            var path =
                    new com.mpp.stellaeomphalos.client.view.ViewSequence(
                            java.util.List.of(
                                    new com.mpp.stellaeomphalos.client.view.ViewSequence.Keyframe(
                                            player.getX(),
                                            player.getEyeY(),
                                            player.getZ(),
                                            0,
                                            player.getYRot(),
                                            player.getXRot(),
                                            70),
                                    new com.mpp.stellaeomphalos.client.view.ViewSequence.Keyframe(
                                            player.getX() + 1,
                                            player.getEyeY() + 1,
                                            player.getZ(),
                                            5,
                                            player.getYRot(),
                                            player.getXRot(),
                                            65)),
                            1);
            if (!com.mpp.stellaeomphalos.client.view.ViewTransformManager.start(path)
                    || mc.getCameraEntity() == player)
                throw new AssertionError("Camera sequence did not start");
        }
        if (ticks == 100) {
            if (com.mpp.stellaeomphalos.client.view.ViewTransformManager.isActive()
                    || mc.getCameraEntity() != mc.player)
                throw new AssertionError("Camera was not restored");
            if (maxVertices == 0 || maxMachines < 3)
                throw new AssertionError(
                        "World geometry absent: vertices="
                                + maxVertices
                                + " machines="
                                + maxMachines);
            renderedFrames = ClientRenderEvents.worldFrames();
            mc.setScreen(new CelestialScreen(null, CelestialScreen.Mode.OBSERVATORY));
        }
        if (ticks == 120) screenshot(mc, "part7-observatory.png");
        if (ticks == 121) mc.setScreen(new AstrolabeScreen());
        if (ticks == 123) screenshot(mc, "part7-astrolabe.png");
        if (ticks == 125) mc.setScreen(new BoonTreeScreen(null));
        if (ticks == 145) {
            if (!(mc.screen instanceof BoonTreeScreen tree))
                throw new AssertionError("Boon screen missing");
            int layouts = tree.layoutCount();
            tree.render(
                    new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource()),
                    0,
                    0,
                    0);
            if (tree.layoutCount() != layouts)
                throw new AssertionError("Rendering rebuilt hit boxes");
            screenshot(mc, "part7-boons.png");
            mc.setScreen(null);
            com.mpp.stellaeomphalos.client.view.ViewCaptureCache.capture(
                    mc.level.dimension().location(), ORIGIN);
            if (com.mpp.stellaeomphalos.client.view.ViewCaptureCache.texture(
                            mc.level.dimension().location(), ORIGIN)
                    == null)
                throw new AssertionError("Screenshot cache failed to load its own capture");
            meshCloses = com.mpp.stellaeomphalos.client.render.obj.ObjMeshLibrary.closedCount();
            reload = mc.reloadResourcePacks();
        }
        if (ticks > 145 && stage == 2 && reload.isDone()) {
            reload.join();
            if (com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.flag("render.staticMeshes")
                    && com.mpp.stellaeomphalos.client.render.obj.ObjMeshLibrary.closedCount()
                            <= meshCloses)
                throw new AssertionError("Static VBO not closed during resource reload");
            if(++reloads<10){ticks=145;reload=mc.reloadResourcePacks();return;}
            stage = 3;
            ticks = 150;
            if (OmphalosRenderTypes.loadedShaderCount() != 5)
                throw new AssertionError("Shaders lost on reload");
        }
        if (stage == 3 && ticks == 160) {
            mc.setScreen(null);
            mc.getSingleplayerServer()
                    .execute(
                            () -> {
                                var server = mc.getSingleplayerServer();
                                var level = server.overworld();
                                var from = ORIGIN.offset(3, 0, 3);
                                var target = ORIGIN.offset(12, 0, 0);
                                level.setBlock(
                                        from,
                                        WorldContent.BLOCKS
                                                .get("gate_core")
                                                .get()
                                                .defaultBlockState(),
                                        3);
                                level.setBlock(
                                        target,
                                        WorldContent.BLOCKS
                                                .get("gate_core")
                                                .get()
                                                .defaultBlockState(),
                                        3);
                                com.mpp.stellaeomphalos.content.world.GateLedger.get(level)
                                        .add(from);
                                com.mpp.stellaeomphalos.content.world.GateLedger.get(level)
                                        .add(target);
                                for (var player : server.getPlayerList().getPlayers()) {
                                    player.teleportTo(
                                            level,
                                            from.getX() + .5,
                                            from.getY() + 1,
                                            from.getZ() + .5,
                                            0,
                                            0);
                                    com.mpp.stellaeomphalos.content.world.GateNetworkService.open(
                                            player, from);
                                }
                            });
        }
        if (stage == 3 && ticks == 180) {
            if (!(mc.screen instanceof GatewayScreen screen))
                throw new AssertionError("Gateway packet did not open screen");
            screen.focusTarget(0);
            screen.charge();
            screenshot(mc, "part7-gateway.png");
        }
        if (stage == 3 && ticks == 220 && mc.player.getX() > 10)
            throw new AssertionError("Gateway teleported before charge completion");
        if (stage == 3 && ticks == 300) {
            if (mc.player.distanceToSqr(12.5, -58, .5) > 4)
                throw new AssertionError("Charged gateway did not reach its offered destination");
            mc.setScreen(null);
            if (ClientRenderEvents.worldFrames() <= renderedFrames)
                throw new AssertionError("World did not render after resource reload");
            ClientRenderEvents.clear();
            if (ClientRenderEvents.EFFECTS.trackCount() != 0
                    || ParticleSpawner.alive() != 0
                    || com.mpp.stellaeomphalos.client.sound.MachineSoundHost.size() != 0
                    || DeferredEffectQueue.size() != 0
                    || com.mpp.stellaeomphalos.client.view.ViewCaptureCache.size() != 0
                    || com.mpp.stellaeomphalos.client.render.obj.ObjMeshLibrary.liveCount() != 0)
                throw new AssertionError("Visual state survived cleanup");
            LogUtils.getLogger()
                    .info(
                            "PART7_CLIENT_SMOKE_PASS: world, {} machines, {} vertices, 5 shaders,"
                                + " particle providers, screens, camera, gateway travel, reload,"
                                + " cleanup, 10 reloads",
                            maxMachines,
                            maxVertices);
            restoreSettings();
            mc.stop();
        }
    }

    private static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(
                mc.gameDirectory,
                name,
                mc.getMainRenderTarget(),
                message -> LogUtils.getLogger().info("Part7 screenshot: {}", message.getString()));
    }
}
