package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.lumen.capability.LumenSink;
import com.mpp.stellaeomphalos.lumen.transport.LumenBroadcast;
import com.mpp.stellaeomphalos.lumen.transport.LumenSession;
import com.mpp.stellaeomphalos.network.toClient.PktLumenDelta;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Receiving end and reusable host base (altar/well/infuser/ritual classes extend it in later parts):
 * a long LU buffer plus a consumption API for the host. Stored/capacity sync via writeClientState;
 * changes above 5% of capacity broadcast at most once per 2 s per node.
 */
public abstract class LumenSinkBlockEntity extends LumenNodeBlockEntity implements LumenSink {
    private static final double ANNOUNCE_RANGE_SQ = 64 * 64;
    private static final long ANNOUNCE_INTERVAL_TICKS = 40;
    private final long capacity;
    private long stored;
    private long lastAnnounceTick = -ANNOUNCE_INTERVAL_TICKS;
    private long lastAnnouncedStored;

    protected LumenSinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity) {
        super(type, pos, state);
        if (capacity < 1) throw new IllegalArgumentException("Nonpositive lumen capacity");
        this.capacity = capacity;
    }

    @Override public final LumenIO io() { return LumenIO.SINK; }

    @Override
    public long acceptLumen(Level level, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long accepted = Math.min(amount, capacity - stored);
        if (accepted > 0 && !simulate) {
            stored += accepted;
            setChanged();
            onLumenAccepted(accepted);
            maybeAnnounce();
        }
        return accepted;
    }

    /** Host consumption API (recipes/machines): drains the buffer, honoring simulate. */
    public long consumeLumen(long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long taken = Math.min(amount, stored);
        if (taken > 0 && !simulate) {
            stored -= taken;
            setChanged();
            maybeAnnounce();
        }
        return taken;
    }

    protected void onLumenAccepted(long amount) {}

    @Override public long lumenCapacity() { return capacity; }
    @Override public long lumenStored() { return stored; }

    private void maybeAnnounce() {
        if (!(level instanceof ServerLevel server)) return;
        long now = server.getGameTime();
        if (now - lastAnnounceTick < ANNOUNCE_INTERVAL_TICKS) return;
        if (Math.abs(stored - lastAnnouncedStored) * 20 <= capacity) return;
        lastAnnounceTick = now;
        lastAnnouncedStored = stored;
        markClientDirty();
        LumenBroadcast.sendToNearby(server, worldPosition,
                new PktLumenDelta(LumenSession.current(), worldPosition, stored, capacity), ANNOUNCE_RANGE_SQ);
    }

    @Override
    protected void writePersistent(CompoundTag tag) {
        super.writePersistent(tag);
        tag.putLong("Stored", stored);
    }

    @Override
    protected void readPersistent(CompoundTag tag) {
        super.readPersistent(tag);
        stored = Math.max(0, Math.min(tag.getLong("Stored"), capacity));
    }

    @Override
    protected void writeClientState(CompoundTag tag) {
        super.writeClientState(tag);
        tag.putLong("Stored", stored);
        tag.putLong("Capacity", capacity);
    }

    @Override
    protected void readClientState(CompoundTag tag) {
        super.readClientState(tag);
        stored = Math.max(0, Math.min(tag.getLong("Stored"), capacity));
    }
}
