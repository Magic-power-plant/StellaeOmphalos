package com.mpp.stellaeomphalos.content.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Distant ambient starfall: finite lifetime, horizontal drift and no chunk tickets. */
public final class StarfallEntity extends Entity {
    private int age;

    public StarfallEntity(EntityType<? extends StarfallEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    protected void defineSynchedData() {}

    protected void readAdditionalSaveData(CompoundTag n) {
        age = Math.max(0, n.getInt("Age"));
    }

    protected void addAdditionalSaveData(CompoundTag n) {
        n.putInt("Age", age);
    }

    public void tick() {
        super.tick();
        if (++age > 600) {
            discard();
            return;
        }
        var next = position().add(getDeltaMovement());
        if (!level().hasChunkAt(net.minecraft.core.BlockPos.containing(next))) {
            discard();
            return;
        }
        setPos(next.x, next.y, next.z);
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
