package com.mpp.stellaeomphalos.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;

/** Six textured cube faces with gravity, collision and a reusable quaternion for tumbling. */
public final class FloatingBlockParticle extends TextureSheetParticle {
    private static final float[] CORNERS={
        -1,-1,-1, -1,-1,1, -1,1,1, -1,1,-1,
        1,-1,1, 1,-1,-1, 1,1,-1, 1,1,1,
        -1,-1,1, -1,-1,-1, 1,-1,-1, 1,-1,1,
        -1,1,-1, -1,1,1, 1,1,1, 1,1,-1,
        1,-1,-1, -1,-1,-1, -1,1,-1, 1,1,-1,
        -1,-1,1, 1,-1,1, 1,1,1, -1,1,1};
    private final org.joml.Quaternionf rotation=new org.joml.Quaternionf();
    private final org.joml.Vector3f vertex=new org.joml.Vector3f();
    private final int ticket;
    private final float spin;
    private boolean released;
    private FloatingBlockParticle(ClientLevel level,double x,double y,double z,double vx,double vy,double vz,boolean fluid,int ticket){
        super(level,x,y,z,vx,vy,vz);this.ticket=ticket;xd=vx;yd=vy;zd=vz;
        gravity=.4F;friction=.96F;hasPhysics=true;lifetime=40+random.nextInt(40);quadSize=.045F+random.nextFloat()*.03F;spin=random.nextFloat()*.12F+.02F;
        if(fluid){
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new net.minecraft.resources.ResourceLocation("minecraft:block/water_still"));
            setSprite(sprite);rCol=.6F;gCol=.8F;bCol=1;alpha=.65F;
        }else{
            var pos=net.minecraft.core.BlockPos.containing(x,y,z);var state=level.hasChunkAt(pos)?level.getBlockState(pos):Blocks.STONE.defaultBlockState();
            if(state.isAir())state=Blocks.STONE.defaultBlockState();
            setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon());
        }
    }
    @Override public void render(VertexConsumer consumer,Camera camera,float partial){
        var eye=camera.getPosition();float px=(float)(Mth.lerp(partial,xo,x)-eye.x),py=(float)(Mth.lerp(partial,yo,y)-eye.y),pz=(float)(Mth.lerp(partial,zo,z)-eye.z);
        rotation.rotationXYZ((age+partial)*spin,(age+partial)*spin*.7F,(age+partial)*spin*.3F);
        int light=getLightColor(partial);float opacity=alpha*Math.max(0,1-(age+partial)/lifetime);
        for(int i=0;i<CORNERS.length;i+=3){vertex.set(CORNERS[i],CORNERS[i+1],CORNERS[i+2]).mul(quadSize).rotate(rotation);
            int corner=(i/3)%4;consumer.vertex(px+vertex.x,py+vertex.y,pz+vertex.z).uv(corner==0||corner==3?getU0():getU1(),corner<2?getV0():getV1())
                    .color(rCol,gCol,bCol,opacity).uv2(light).endVertex();}
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.TERRAIN_SHEET;}
    @Override public void remove(){super.remove();if(!released){released=true;ParticleSpawner.release(ticket);}}
    public static ParticleProvider<SimpleParticleType> provider(boolean fluid){return(type,level,x,y,z,vx,vy,vz)->{
        int ticket=ParticleSpawner.acquire(x,y,z,fluid);return ticket<0?null:new FloatingBlockParticle(level,x,y,z,vx,vy,vz,fluid,ticket);
    };}
}
