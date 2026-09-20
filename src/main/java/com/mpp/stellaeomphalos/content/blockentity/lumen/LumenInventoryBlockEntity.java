package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.content.blockentity.InventoryBlockEntity;
import com.mpp.stellaeomphalos.lumen.capability.LumenCapability;
import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.lumen.capability.LumenNode;
import com.mpp.stellaeomphalos.lumen.transport.LumenMath;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/** Lumen node with an item handler (lens/crystal slots); the inventory never leaks into client sync. */
public abstract class LumenInventoryBlockEntity extends InventoryBlockEntity implements LumenNode {
    private final LumenGlue glue = new LumenGlue(this, this::lumenStored, this::lumenCapacity);

    protected LumenInventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, EnumSet<Direction> faces) {
        super(type, pos, state, slots, faces);
    }

    @Override public LumenIO io() { return LumenIO.NONE; }
    @Override public final BlockPos pos() { return worldPosition; }
    @Override public final int sectionY() {
        return LumenMath.sectionY(worldPosition.getY(), level != null ? level.getMinBuildHeight() : 0);
    }
    @Override public final boolean onNeighborChanged(Level level, BlockPos changed) { return glue.neighborChanged(level); }
    @Override public final boolean needsRebuild() { return glue.needsRebuild(); }
    @Override public final void markClean() { glue.markClean(); }

    protected long lumenStored() { return 0L; }
    protected long lumenCapacity() { return 0L; }
    protected final void markLumenDirty() { glue.markDirty(); }

    @Override public void onLoad() { super.onLoad(); glue.join(this); }
    @Override public void setRemoved() { super.setRemoved(); glue.leave(this); }
    @Override public void onChunkUnloaded() { super.onChunkUnloaded(); glue.leave(this); }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved() && cap == LumenCapability.LUMEN) return glue.capability().cast();
        return super.getCapability(cap, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); glue.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); glue.revive(); }

    @Override protected void writeMachineState(CompoundTag tag) {}
    @Override protected void readMachineState(CompoundTag tag) {}
    @Override protected void writeClientState(CompoundTag tag) {}
    @Override protected void readClientState(CompoundTag tag) {}
}
