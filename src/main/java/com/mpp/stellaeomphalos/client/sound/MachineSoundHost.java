package com.mpp.stellaeomphalos.client.sound;

import com.mpp.stellaeomphalos.content.blockentity.crafting.AbstractCraftingMachine;
import com.mpp.stellaeomphalos.content.blockentity.lumen.LumenContent;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.IdentityHashMap;
import java.util.Map;

/** Tick-owned sound lifetime; nearby loaded chunks are sampled without forcing chunk loads. */
public final class MachineSoundHost {
    private static final Map<BlockEntity, LoopingMachineSound> HOSTS = new IdentityHashMap<>();
    private static int scanCursor;

    private MachineSoundHost() {}

    public static void observe(BlockEntity be) {
        if (HOSTS.containsKey(be) || HOSTS.size() >= 128 || !working(be)) return;
        String id;
        if (be instanceof AbstractCraftingMachine machine) {
            id =
                    switch (machine.machineKind()) {
                        case "asterism" -> "altar_craft_loop";
                        case "quern" -> "grindstone_grind_loop";
                        case "lumen_infuser" -> "infuser_craft_loop";
                        case "lumen_well" -> "well_liquid_loop";
                        default -> null;
                    };
        } else id = "lumen_collect_loop";
        if (id == null) return;
        var event =
                net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                        new ResourceLocation("stellaeomphalos", id));
        if (event == null) return;
        var sound = new LoopingMachineSound(event, be.getBlockPos(), () -> working(be));
        HOSTS.put(be, sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private static boolean working(BlockEntity be) {
        var mc = Minecraft.getInstance();
        if (be.isRemoved()
                || be.getLevel() == null
                || be.getLevel() != mc.level
                || !mc.level.hasChunkAt(be.getBlockPos())
                || mc.player == null
                || be.getBlockPos().distToCenterSqr(mc.player.position()) > 4096) return false;
        return be instanceof AbstractCraftingMachine machine
                ? machine.visualWorking()
                : be instanceof LumenContent.Collector collector && collector.data().seesSky();
    }

    public static void tick() {
        var mc = Minecraft.getInstance();
        var iterator = HOSTS.values().iterator();
        while (iterator.hasNext()) {
            var sound = iterator.next();
            sound.tick();
            if (sound.isStopped()) {
                mc.getSoundManager().stop(sound);
                iterator.remove();
            }
        }
        if (mc.level == null || mc.player == null) return;
        int cx = mc.player.blockPosition().getX() >> 4, cz = mc.player.blockPosition().getZ() >> 4;
        for (int i = 0; i < 9; i++) {
            int index = scanCursor++ % 81;
            var chunk =
                    mc.level
                            .getChunkSource()
                            .getChunk(
                                    cx + index % 9 - 4,
                                    cz + index / 9 - 4,
                                    ChunkStatus.FULL,
                                    false);
            if (chunk != null)
                for (var be : chunk.getBlockEntities().values()) {
                    observe(be);
                    if (be
                            instanceof
                            com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity
                                            technical)
                        com.mpp.stellaeomphalos.client.render.PhantomBatch.prepare(
                                technical.hostState());
                }
        }
    }

    public static void clear() {
        for (var sound : HOSTS.values()) {
            sound.finish();
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
        HOSTS.clear();
        scanCursor = 0;
    }

    public static int size() {
        return HOSTS.size();
    }
}
