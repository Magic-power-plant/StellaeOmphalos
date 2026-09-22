package com.mpp.stellaeomphalos.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.function.BooleanSupplier;

/** An explicit stop predicate is evaluated every tick, including unloaded-machine invalidation. */
public final class LoopingMachineSound extends AbstractTickableSoundInstance {
    private final BooleanSupplier alive;
    private java.util.function.Supplier<net.minecraft.world.phys.Vec3> source;

    public LoopingMachineSound(SoundEvent event, BlockPos pos, BooleanSupplier alive) {
        super(event, SoundSource.BLOCKS, RandomSource.create());
        this.alive = alive;
        looping = true;
        delay = 0;
        volume = .3F;
        pitch = 1;
        x = pos.getX() + .5;
        y = pos.getY() + .5;
        z = pos.getZ() + .5;
    }

    public LoopingMachineSound follow(java.util.function.Supplier<net.minecraft.world.phys.Vec3> source){this.source=source;return this;}
    @Override
    public void tick() {
        if (!alive.getAsBoolean()) {stop();return;}
        if(source!=null){var pos=source.get();x=pos.x;y=pos.y;z=pos.z;}
    }

    public void finish() {
        stop();
    }
}
