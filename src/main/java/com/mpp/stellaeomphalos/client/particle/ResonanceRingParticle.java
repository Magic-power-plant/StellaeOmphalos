package com.mpp.stellaeomphalos.client.particle;

import com.mpp.stellaeomphalos.content.particle.ResonanceRingOptions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;

public final class ResonanceRingParticle extends TextureSheetParticle {
    private final float radius;
    private final int ticket;
    private boolean released;

    private ResonanceRingParticle(
            ResonanceRingOptions options,
            ClientLevel level,
            double x,
            double y,
            double z,
            SpriteSet sprites,
            int ticket) {
        super(level, x, y, z);
        this.ticket = ticket;
        radius = options.radius();
        lifetime = 25;
        hasPhysics = false;
        rCol = (options.color() >> 16 & 255) / 255F;
        gCol = (options.color() >> 8 & 255) / 255F;
        bCol = (options.color() & 255) / 255F;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        quadSize = radius * age / lifetime;
        alpha = Math.max(0, 1F - (float) age / lifetime);
    }

    @Override
    public void remove() {
        super.remove();
        if (!released) {
            released = true;
            ParticleSpawner.release(ticket);
        }
    }

    @Override
    public int getLightColor(float partial) {
        return 0xf000f0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static ParticleProvider<ResonanceRingOptions> provider(SpriteSet sprites) {
        return (options, level, x, y, z, vx, vy, vz) -> {
            int ticket = ParticleSpawner.acquire(x, y, z, false);
            return ticket < 0
                    ? null
                    : new ResonanceRingParticle(options, level, x, y, z, sprites, ticket);
        };
    }
}
