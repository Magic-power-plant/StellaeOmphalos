package com.mpp.stellaeomphalos.content.blockentity.rite;

import com.mpp.stellaeomphalos.content.block.TechnicalBlock;
import com.mpp.stellaeomphalos.content.world.WorldContent;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared technical host lifecycle; links are bounded, loaded-only and never recursive. */
public final class TechnicalBlockEntity extends BlockEntity {
    private BlockPos host;
    private BlockState mimic = Blocks.STONE.defaultBlockState();
    private long ticks;
    private int boreY;
    private boolean active;

    public TechnicalBlockEntity(BlockPos p, BlockState s) {
        super(WorldContent.TECH_ENTITY.get(), p, s);
        boreY = p.getY() - 2;
    }

    public void host(BlockPos position) {
        if (position.equals(worldPosition) || position.distSqr(worldPosition) > 4096)
            throw new IllegalArgumentException("Invalid host link");
        host = position.immutable();
        setChanged();
    }

    public BlockPos host() {
        return host;
    }

    public void mimic(BlockState s) {
        if (s.getBlock() instanceof TechnicalBlock)
            throw new IllegalArgumentException("Recursive mimic");
        mimic = s;
        setChanged();
    }

    public BlockState hostState() {
        if (level != null && host != null && level.hasChunkAt(host)) {
            var s = level.getBlockState(host);
            if (!(s.getBlock() instanceof TechnicalBlock)) return s;
        }
        return mimic;
    }

    public InteractionResult interact(Player player, InteractionHand hand, BlockHitResult hit) {
        var kind = ((TechnicalBlock) getBlockState().getBlock()).kind();
        if (kind.equals("frame_shell") && host != null && level.hasChunkAt(host)) {
            var target = level.getBlockState(host);
            if (!(target.getBlock() instanceof TechnicalBlock))
                return target.use(
                        level,
                        player,
                        hand,
                        new BlockHitResult(
                                hit.getLocation(), hit.getDirection(), host, hit.isInside()));
        }
        if (!level.isClientSide) {
            active = !active;
            setChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        ticks++;
        if (!active || ticks % 20 != 0) return;
        String kind = ((TechnicalBlock) getBlockState().getBlock()).kind();
        if (kind.equals("bore_core")) {
            var below = worldPosition.below();
            if (!(server.getBlockState(below).getBlock() instanceof TechnicalBlock head)
                    || !head.kind().startsWith("bore_head_")) return;
            var target = new BlockPos(worldPosition.getX(), boreY, worldPosition.getZ());
            if (server.isOutsideBuildHeight(target)) {
                active = false;
                return;
            }
            if (server.hasChunkAt(target)) {
                var s = server.getBlockState(target);
                if (s.getDestroySpeed(server, target) >= 0) server.destroyBlock(target, true);
                boreY--;
                setChanged();
            }
        }
        if (kind.equals("mineral_regenerator")
                && server.getBlockState(worldPosition.above()).isAir()
                && server.random.nextInt(20) == 0) {
            var selected =
                    com.mpp.stellaeomphalos.content.world.WorldBehaviorRegistry.randomOre(
                            server.random, "mineral");
            if (selected != null) server.setBlock(worldPosition.above(), selected, 3);
        }
    }

    protected void saveAdditional(CompoundTag n) {
        super.saveAdditional(n);
        if (host != null) n.putLong("Host", host.asLong());
        n.put("Mimic", NbtUtils.writeBlockState(mimic));
        n.putLong("Ticks", ticks);
        n.putInt("BoreY", boreY);
        n.putBoolean("Active", active);
    }

    public void load(CompoundTag n) {
        super.load(n);
        host = n.contains("Host") ? BlockPos.of(n.getLong("Host")) : null;
        mimic =
                NbtUtils.readBlockState(
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),
                        n.getCompound("Mimic"));
        if (mimic.getBlock() instanceof TechnicalBlock) mimic = Blocks.STONE.defaultBlockState();
        ticks = n.getLong("Ticks");
        boreY = n.contains("BoreY") ? n.getInt("BoreY") : worldPosition.getY() - 2;
        active = n.getBoolean("Active");
    }
}
