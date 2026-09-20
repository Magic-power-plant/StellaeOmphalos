package com.mpp.stellaeomphalos.client.structure;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.world.WorldContent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Omphalos.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class WorldEntityRenderers {
    private WorldEntityRenderers() {}

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        var type =
                (net.minecraft.world.entity.EntityType<
                                com.mpp.stellaeomphalos.content.entity.StarfallEntity>)
                        net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                                WorldContent.STARFALL.id());
        event.registerEntityRenderer(
                java.util.Objects.requireNonNull(type),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
    }
}
