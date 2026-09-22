package com.mpp.stellaeomphalos.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** Shared sprite particle with distinct profiles; frame selection uses the wall clock. */
public final class LumenParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final int ticket;
    private final long born = net.minecraft.Util.getMillis();
    private boolean released;

    private LumenParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            SpriteSet sprites,
            String profile,
            int ticket) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.ticket = ticket;
        xd = vx;
        yd = vy;
        zd = vz;
        friction = .97F;
        lifetime = 30 + random.nextInt(20);
        quadSize = .06F;
        hasPhysics = false;
        rCol = .65F;
        gCol = .83F;
        bCol = 1;
        pickSprite(sprites);
        if (profile.contains("fleck")
                || profile.contains("dust")
                || profile.contains("ash")
                || profile.contains("cube")) {
            gravity = .15F;
            hasPhysics = true;
            rCol = .78F;
            gCol = .7F;
            bCol = .9F;
        }
        if (profile.contains("grow")) {
            lifetime = 60;
            quadSize = .1F;
            yd = .02;
        }
        if (profile.contains("orbital") || profile.contains("mote")) {
            rCol = .8F;
            gCol = .72F;
        }
    }

    @Override
    public void tick() {
        super.tick();
        alpha = Math.max(0, 1 - (float) age / lifetime);
    }

    @Override
    public void render(VertexConsumer v, Camera camera, float partial) {
        setSprite(sprites.get((int) ((net.minecraft.Util.getMillis() - born) / 50 % 8), 7));
        super.render(v, camera, partial);
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
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, String profile) {
        return (type, level, x, y, z, vx, vy, vz) -> {
            int ticket =
                    ParticleSpawner.acquire(
                            x, y, z, profile.contains("cube") || profile.contains("grow"));
            return ticket < 0
                    ? null
                    : new LumenParticle(level, x, y, z, vx, vy, vz, sprites, profile, ticket);
        };
    }
}
