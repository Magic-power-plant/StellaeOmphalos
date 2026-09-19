package com.mpp.stellaeomphalos.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Owns persistence and client synchronization channels; inventory is never sent implicitly. */
public abstract class SyncedBlockEntity extends BlockEntity {
    protected SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    protected abstract void writePersistent(CompoundTag tag);
    protected abstract void readPersistent(CompoundTag tag);
    protected abstract void writeClientState(CompoundTag tag);
    protected abstract void readClientState(CompoundTag tag);
    @Override protected final void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); writePersistent(tag); }
    @Override public final void load(CompoundTag tag) { super.load(tag); readPersistent(tag); }
    @Override public final CompoundTag getUpdateTag() { var tag = new CompoundTag(); writeClientState(tag); return tag; }
    @Override public final void handleUpdateTag(CompoundTag tag) { readClientState(tag); }
    @Override public final ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public final void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) readClientState(packet.getTag());
    }
    protected final void markClientDirty() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
}
