package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.content.blockentity.SkyAwareBlockEntity;
import com.mpp.stellaeomphalos.lumen.capability.LumenCapability;
import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.lumen.capability.LumenNode;
import com.mpp.stellaeomphalos.lumen.transport.LumenMath;
import com.mpp.stellaeomphalos.lumen.transport.LumenTopology;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/** Lumen node with cached sky visibility; a visibility change marks the source dirty in the topology. */
public abstract class SkyboundLumenBlockEntity extends SkyAwareBlockEntity implements LumenNode {
    private final LumenGlue glue = new LumenGlue(this, this::lumenStored, this::lumenCapacity);
    private boolean hadSky;

    protected SkyboundLumenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

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

    @Override
    protected final void tickWithSky(boolean visible) {
        if (visible != hadSky) {
            hadSky = visible;
            glue.markDirty();
            if (level instanceof ServerLevel server) LumenTopology.markSourceDirty(server, worldPosition);
            onSkyVisibilityChanged(visible);
        }
        tickSkybound(visible);
    }

    protected void onSkyVisibilityChanged(boolean visible) {}
    protected void tickSkybound(boolean skyVisible) {}

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

    @Override protected void writePersistent(CompoundTag tag) {}
    @Override protected void readPersistent(CompoundTag tag) {}
    @Override protected void writeClientState(CompoundTag tag) {}
    @Override protected void readClientState(CompoundTag tag) {}
}
