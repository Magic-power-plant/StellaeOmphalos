package com.mpp.stellaeomphalos.client.sound;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
/** Moving meteor sounds share explicit stop semantics with block-bound sounds. */
public final class EntitySoundHost {
    private static final java.util.Map<Entity,LoopingMachineSound> SOUNDS=new java.util.IdentityHashMap<>();
    private EntitySoundHost(){}
    private static boolean active(Entity entity){var mc=Minecraft.getInstance();return !entity.isRemoved()&&entity.level()==mc.level&&mc.player!=null&&entity.distanceToSqr(mc.player)<96*96;}
    public static void tick(){
        var mc=Minecraft.getInstance();
        var iterator=SOUNDS.entrySet().iterator();
        while(iterator.hasNext()){var entry=iterator.next();entry.getValue().tick();if(entry.getValue().isStopped()){mc.getSoundManager().stop(entry.getValue());iterator.remove();}}
        if(mc.level==null||mc.player==null)return;
        for(var entity:mc.level.entitiesForRendering()){
            if(SOUNDS.size()>=8)break;
            if(!(entity instanceof com.mpp.stellaeomphalos.content.entity.catalog.FallingStarEntity || entity instanceof com.mpp.stellaeomphalos.content.entity.StarfallEntity)
                    ||SOUNDS.containsKey(entity)||!active(entity))continue;
            var sound=new LoopingMachineSound(com.mpp.stellaeomphalos.content.particle.ClientVisualContent.sound("meteor_fall"),entity.blockPosition(),()->active(entity)).follow(entity::position);
            SOUNDS.put(entity,sound);mc.getSoundManager().play(sound);
        }
    }
    public static void clear(){for(var sound:SOUNDS.values()){sound.finish();Minecraft.getInstance().getSoundManager().stop(sound);}SOUNDS.clear();}
}
