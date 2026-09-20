package com.mpp.stellaeomphalos.client.codex;

import com.mpp.stellaeomphalos.OmphalosConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.*;

/** Local visual capabilities; no world edits and no exact-coordinate network projection. */
public final class MantleVisuals {
    private MantleVisuals() {}

    public static void attach() {
        MinecraftForge.EVENT_BUS.addListener(MantleVisuals::tick);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        var mc = Minecraft.getInstance();
        if (e.phase != TickEvent.Phase.END || mc.player == null || mc.level == null) return;
        var stack = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .toString()
                        .equals("stellaeomphalos:star_mantle")
                || !stack.hasTag()) return;
        String sign = stack.getTag().getString("SignId");
        if (sign.endsWith(":vicio")
                && (mc.player.getAbilities().flying || mc.player.isFallFlying())) {
            var pos = mc.player.position().subtract(mc.player.getDeltaMovement().scale(1.5));
            int count = mc.options.getCameraType().isFirstPerson() ? 1 : 2;
            for (int i = 0; i < count; i++)
                mc.level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 0, .01, 0);
        }
        if (!OmphalosConfig.CLIENT.flag("mantle.enableClientSideDetection")
                || mc.player.tickCount % 10 != 0) return;
        if (sign.endsWith(":lucerna")) {
            for (var entity :
                    mc
                            .level
                            .getEntities(
                                    mc.player,
                                    mc.player.getBoundingBox().inflate(16),
                                    x -> x instanceof net.minecraft.world.entity.LivingEntity)
                            .stream()
                            .limit(32)
                            .toList())
                if (entity.distanceToSqr(mc.player) >= 36)
                    mc.level.addParticle(
                            ParticleTypes.END_ROD,
                            entity.getX(),
                            entity.getY() + 1,
                            entity.getZ(),
                            0,
                            .02,
                            0);
            var chunk =
                    mc.level
                            .getChunkSource()
                            .getChunkNow(mc.player.chunkPosition().x, mc.player.chunkPosition().z);
            if (chunk != null)
                for (var be : chunk.getBlockEntities().values())
                    if (be instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity
                            && be.getBlockPos().distSqr(mc.player.blockPosition()) >= 36)
                        mc.level.addParticle(
                                ParticleTypes.ENCHANT,
                                be.getBlockPos().getX() + .5,
                                be.getBlockPos().getY() + .5,
                                be.getBlockPos().getZ() + .5,
                                0,
                                .1,
                                0);
        }
        if (sign.endsWith(":mineralis")
                && mc.player.getMainHandItem().getItem() instanceof BlockItem block) {
            for (int i = 0; i < 64; i++) {
                var p =
                        mc.player
                                .blockPosition()
                                .offset(
                                        mc.level.random.nextInt(17) - 8,
                                        mc.level.random.nextInt(9) - 4,
                                        mc.level.random.nextInt(17) - 8);
                if (mc.level.hasChunkAt(p) && mc.level.getBlockState(p).is(block.getBlock()))
                    mc.level.addParticle(
                            ParticleTypes.ENCHANT,
                            p.getX() + .5,
                            p.getY() + .8,
                            p.getZ() + .5,
                            0,
                            -.02,
                            0);
            }
        }
    }
}
