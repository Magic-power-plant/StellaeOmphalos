package com.mpp.stellaeomphalos.client;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.client.render.ber.MachineRenderers.*;
import com.mpp.stellaeomphalos.content.block.MachineContent;
import com.mpp.stellaeomphalos.content.blockentity.crafting.CraftingContent;
import com.mpp.stellaeomphalos.content.world.WorldContent;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
        modid = Omphalos.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ClientBootstrap {
    private ClientBootstrap() {}

    private static <T extends BlockEntity> BlockEntityType<T> blockEntity(
            RegistrationGuard<BlockEntityType<T>> guard) {
        var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(guard.id());
        if (type == null)
            throw new IllegalStateException("Missing block entity renderer target " + guard.id());
        @SuppressWarnings("unchecked")
        var cast = (BlockEntityType<T>) type;
        return cast;
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(
                blockEntity(CraftingContent.ALTAR_ENTITY), c -> new AsterismAltarRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(CraftingContent.RELAY_ENTITY), c -> new ResonanceRelayRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(CraftingContent.INFUSER_ENTITY), c -> new InfuserRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(CraftingContent.CHALICE_ENTITY), c -> new ChaliceRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(CraftingContent.WELL_ENTITY), c -> new LumenWellRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(CraftingContent.GRINDWHEEL_ENTITY), c -> new GrindstoneRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(
                        com.mpp.stellaeomphalos.content.blockentity.lumen.LumenContent.COLLECTOR),
                c -> new CollectorRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(MachineContent.RESONANCE_ENTITY), c -> new ResonanceAltarRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(MachineContent.BEAM_LENS_ENTITY), c -> new LensRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(MachineContent.BEAM_PRISM_ENTITY), c -> new PrismRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(MachineContent.STAR_CHART_ENTITY), c -> new StarChartTableRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(WorldContent.PEDESTAL_ENTITY), c -> new RitePedestalRenderer());
        e.registerBlockEntityRenderer(
                blockEntity(WorldContent.TECH_ENTITY),
                c -> new com.mpp.stellaeomphalos.client.render.ber.TechnicalRenderer());
        com.mpp.stellaeomphalos.client.render.entity.VisualEntityRenderers.register(e);
    }

    @SubscribeEvent
    public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
        e.enqueueWork(
                () -> {
                    com.mpp.stellaeomphalos.client.render.item.StackRenderBypass.install();
                    OmphalosClient.handlers()
                            .register(
                                    com.mpp
                                            .stellaeomphalos
                                            .network
                                            .toClient
                                            .PktGatewayTargets
                                            .class,
                                    (mc, packet) -> {
                                        if (mc.level != null
                                                && mc.level
                                                        .dimension()
                                                        .location()
                                                        .equals(packet.dimension())) {
                                            com.mpp.stellaeomphalos.client.view.ViewCaptureCache
                                                    .capture(packet.dimension(), packet.origin());
                                            mc.setScreen(
                                                    new com.mpp.stellaeomphalos.client.screen
                                                            .GatewayScreen(packet));
                                        }
                                    });
                    OmphalosClient.handlers()
                            .register(
                                    com.mpp.stellaeomphalos.network.toClient.PktViewSequence.class,
                                    (mc, packet) -> {
                                        if (mc.level == null
                                                || !mc.level
                                                        .dimension()
                                                        .location()
                                                        .equals(packet.dimension())) return;
                                        var points =
                                                packet.points().stream()
                                                        .map(
                                                                p ->
                                                                        new com.mpp.stellaeomphalos
                                                                                .client.view
                                                                                .ViewSequence
                                                                                .Keyframe(
                                                                                p.x(), p.y(), p.z(),
                                                                                p.tick(), p.yaw(),
                                                                                p.pitch(), p.fov()))
                                                        .toList();
                                        if (com.mpp.stellaeomphalos.client.view.ViewTransformManager
                                                .start(
                                                        new com.mpp.stellaeomphalos.client.view
                                                                .ViewSequence(
                                                                points, packet.priority())))
                                            com.mpp.stellaeomphalos.client.sound.UiSounds.play(
                                                    "view_sequence_whoosh");
                                    });
                    OmphalosClient.handlers()
                            .register(
                                    com.mpp
                                            .stellaeomphalos
                                            .network
                                            .toClient
                                            .PktOpenObservation
                                            .class,
                                    (mc, packet) -> {
                                        if (mc.player == null) return;
                                        if (packet.route().equals("astrolabe")) {
                                            mc.setScreen(
                                                    new com.mpp.stellaeomphalos.client.screen
                                                            .AstrolabeScreen());
                                            return;
                                        }
                                        var mode =
                                                switch (packet.route()) {
                                                    case "hand_telescope" ->
                                                            com.mpp.stellaeomphalos.client.screen
                                                                    .CelestialScreen.Mode
                                                                    .HAND_TELESCOPE;
                                                    case "lore_scroll" ->
                                                            com.mpp.stellaeomphalos.client.screen
                                                                    .CelestialScreen.Mode
                                                                    .LORE_SCROLL;
                                                    case "sign_scroll" ->
                                                            com.mpp.stellaeomphalos.client.screen
                                                                    .CelestialScreen.Mode
                                                                    .SIGN_SCROLL;
                                                    default -> null;
                                                };
                                        if (mode != null)
                                            mc.setScreen(
                                                    new com.mpp.stellaeomphalos.client.screen
                                                                    .CelestialScreen(null, mode)
                                                            .focus(packet.sign().orElse(null))
                                                            .sources(packet.signs()));
                                    });
                    net.minecraft.client.gui.screens.MenuScreens.register(
                            com.mpp.stellaeomphalos.content.menu.StationMenus.SPYGLASS.get(),
                            com.mpp.stellaeomphalos.client.screen.StationScreens.Spyglass::new);
                    net.minecraft.client.gui.screens.MenuScreens.register(
                            com.mpp.stellaeomphalos.content.menu.StationMenus.OBSERVATORY.get(),
                            com.mpp.stellaeomphalos.client.screen.StationScreens.Observatory::new);
                    net.minecraft.client.gui.screens.MenuScreens.register(
                            com.mpp.stellaeomphalos.content.menu.StationMenus.STAR_CHART_TABLE
                                    .get(),
                            com.mpp.stellaeomphalos.client.screen.StationScreens.Chart::new);
                });
    }

    @SubscribeEvent
    public static void itemModels(ModelEvent.ModifyBakingResult event) {
        com.mpp.stellaeomphalos.client.render.item.StackRenderBypass.bake(event);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer =
                    event.getSkin(skin);
            if (renderer != null)
                renderer.addLayer(
                        new com.mpp.stellaeomphalos.client.render.model.MantlePlayerLayer(
                                renderer));
        }
    }

    @SubscribeEvent
    public static void dimensionEffects(RegisterDimensionSpecialEffectsEvent e) {
        e.register(
                new net.minecraft.resources.ResourceLocation("stellaeomphalos:starfield"),
                new com.mpp.stellaeomphalos.client.sky.StarfieldDimensionEffects());
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent e) throws java.io.IOException {
        com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes.register(e);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent e) {
        @SuppressWarnings("unchecked")
        var ring =
                (net.minecraft.core.particles.ParticleType<
                                com.mpp.stellaeomphalos.content.particle.ResonanceRingOptions>)
                        net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(
                                com.mpp.stellaeomphalos.content.particle.ClientVisualContent
                                        .RESONANCE_RING
                                        .id());
        e.registerSpriteSet(
                java.util.Objects.requireNonNull(ring),
                com.mpp.stellaeomphalos.client.particle.ResonanceRingParticle::provider);
        for (var id : com.mpp.stellaeomphalos.core.registry.ModParticles.ENTRIES.ids()) {
            var type = net.minecraftforge.registries.ForgeRegistries.PARTICLE_TYPES.getValue(id);
            if (type instanceof net.minecraft.core.particles.SimpleParticleType simple
                    && java.util.Set.of("floating_cube","translucent_falling_block","fountain_droplet","phantom_fleck","grindstone_fleck").contains(id.getPath())) {
                e.registerSpecial(simple,com.mpp.stellaeomphalos.client.particle.FloatingBlockParticle.provider(id.getPath().equals("floating_cube")||id.getPath().equals("fountain_droplet")));
            } else if (type instanceof net.minecraft.core.particles.SimpleParticleType simple)
                e.registerSpriteSet(
                        simple,
                        sprites ->
                                com.mpp.stellaeomphalos.client.particle.LumenParticle.provider(
                                        sprites, id.getPath()));
        }
    }

    @SubscribeEvent
    public static void atlas(net.minecraftforge.client.event.TextureStitchEvent.Post event) {
        if (event.getAtlas()
                .location()
                .equals(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS))
            com.mpp.stellaeomphalos.client.render.util.WorldDraw.refreshAtlas();
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener(
                (net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                        resources -> {
                            com.mpp.stellaeomphalos.client.render.obj.ObjMeshLibrary.reload(
                                    resources);
                            com.mpp.stellaeomphalos.client.view.ViewCaptureCache.clear();
                            com.mpp.stellaeomphalos.client.image.PaletteTable.reload(resources);
                            com.mpp.stellaeomphalos.client.particle.ParticleSpawner.clear();
                            com.mpp.stellaeomphalos.client.render.res.TextureStore.clear();
                            com.mpp.stellaeomphalos.client.render.PhantomBatch.clear();
                            com.mpp.stellaeomphalos.client.sky.StarfieldRenderer.clear();
                            com.mpp.stellaeomphalos.client.render.ShaderCompat.refresh();
                        });
    }
}
