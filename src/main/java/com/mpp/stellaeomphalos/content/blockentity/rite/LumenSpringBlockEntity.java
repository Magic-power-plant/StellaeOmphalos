package com.mpp.stellaeomphalos.content.blockentity.rite;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.content.world.WorldContent;
import com.mpp.stellaeomphalos.content.world.capability.WorldCapabilities;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/** A 1-bucket buffer supports vanilla bucket transfer while pumping respects the per-tick quota. */
public final class LumenSpringBlockEntity extends BlockEntity {
    private final FluidTank tank =
            new FluidTank(1000) {
                protected void onContentsChanged() {
                    setChanged();
                }
            };
    private final IFluidHandler output =
            new IFluidHandler() {
                public int getTanks() {
                    return 1;
                }

                public FluidStack getFluidInTank(int n) {
                    return tank.getFluid().copy();
                }

                public int getTankCapacity(int n) {
                    return 1000;
                }

                public boolean isFluidValid(int n, FluidStack s) {
                    return false;
                }

                public int fill(FluidStack s, FluidAction a) {
                    return 0;
                }

                public FluidStack drain(int max, FluidAction a) {
                    return tank.drain(Math.min(1000, max), a);
                }

                public FluidStack drain(FluidStack s, FluidAction a) {
                    return s.isFluidEqual(tank.getFluid())
                            ? drain(s.getAmount(), a)
                            : FluidStack.EMPTY;
                }
            };
    private LazyOptional<IFluidHandler> capability = LazyOptional.of(() -> output);

    public LumenSpringBlockEntity(BlockPos p, BlockState s) {
        super(WorldContent.SPRING_ENTITY.get(), p, s);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server) || tank.getSpace() == 0) return;
        var chunk =
                server.getChunkSource()
                        .getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        if (chunk == null) return;
        chunk.getCapability(WorldCapabilities.SPRING)
                .ifPresent(
                        vein -> {
                            if (!vein.present()) return;
                            var fluid =
                                    net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(
                                            vein.fluidId());
                            if (fluid == null) {
                                vein.invalidate();
                                return;
                            }
                            int limit =
                                    Math.min(
                                            OmphalosConfig.SERVER.integer(
                                                    "worldgen.springPumpRate"),
                                            tank.getSpace());
                            var requested = new FluidStack(fluid, vein.drain(limit, false));
                            int accepted = tank.fill(requested, IFluidHandler.FluidAction.SIMULATE);
                            if (accepted > 0) {
                                requested.setAmount(vein.drain(accepted, true));
                                tank.fill(requested, IFluidHandler.FluidAction.EXECUTE);
                                if (server.getGameTime() % 80 == 0) {
                                    var sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                                            new net.minecraft.resources.ResourceLocation("stellaeomphalos", "spring_draw"));
                                    if (sound != null) server.playSound(null, worldPosition, sound, net.minecraft.sounds.SoundSource.BLOCKS, .35F, 1);
                                }
                            }
                        });
    }

    public <T> LazyOptional<T> getCapability(Capability<T> c, Direction side) {
        return c == ForgeCapabilities.FLUID_HANDLER
                ? capability.cast()
                : super.getCapability(c, side);
    }

    public void invalidateCaps() {
        super.invalidateCaps();
        capability.invalidate();
    }

    public void reviveCaps() {
        super.reviveCaps();
        capability = LazyOptional.of(() -> output);
    }

    protected void saveAdditional(CompoundTag n) {
        super.saveAdditional(n);
        n.put("Tank", tank.writeToNBT(new CompoundTag()));
    }

    public void load(CompoundTag n) {
        super.load(n);
        tank.readFromNBT(n.getCompound("Tank"));
    }
}
