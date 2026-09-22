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
    public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        var targets = new java.util.ArrayList<net.minecraft.world.item.Item>();
        for (String id : java.util.List.of("lens_blank", "prism_lens", "luminary_rod")) {
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation(Omphalos.MODID, id));
            if (item != null) targets.add(item);
        }
        if (!targets.isEmpty()) event.register((stack, layer) ->
                layer == 0 && stack.hasTag() && stack.getTag().contains("LensColor")
                        ? 0xFF000000 | stack.getTag().getInt("LensColor") : -1,
                targets.toArray(net.minecraft.world.item.Item[]::new));
    }

}
