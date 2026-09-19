package com.mpp.stellaeomphalos.lumen.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Owns single-fluid identity and exact fractional storage; capability operations expose whole mB. */
public abstract class FluidStorage implements IFluidHandler {
    private final int capacity;
    private final long scale;
    private final Runnable changed;
    private FluidStack identity = FluidStack.EMPTY;
    private long units;
    protected FluidStorage(int capacity, long scale, Runnable changed) {
        if (capacity < 1 || scale < 1) throw new IllegalArgumentException("Invalid tank capacity/scale");
        Math.multiplyExact((long) capacity, scale);
        this.capacity = capacity; this.scale = scale; this.changed = java.util.Objects.requireNonNull(changed);
    }
    protected final long fillUnits(FluidStack fluid, long requested, FluidAction action) {
        if (fluid.isEmpty() || requested <= 0 || !isFluidValid(0, fluid)) return 0;
        if (!identity.isEmpty() && !identity.isFluidEqual(fluid)) return 0;
        long accepted = Math.min(requested, capacity * scale - units);
        if (accepted > 0 && action.execute()) {
            identity = fluid.copy(); identity.setAmount(1); units += accepted; changed.run();
        }
        return accepted;
    }
    protected final long drainUnits(long requested, FluidAction action) {
        if (requested <= 0) return 0;
        long removed = Math.min(requested, units);
        if (removed > 0 && action.execute()) { units -= removed; if (units == 0) identity = FluidStack.EMPTY; changed.run(); }
        return removed;
    }
    public final long storedUnits() { return units; }
    protected final long scale() { return scale; }
    @Override public final int getTanks() { return 1; }
    private void checkTank(int tank) { if (tank != 0) throw new IndexOutOfBoundsException(tank); }
    @Override public final int getTankCapacity(int tank) { checkTank(tank); return capacity; }
    @Override public final FluidStack getFluidInTank(int tank) {
        checkTank(tank);
        if (units < scale) return FluidStack.EMPTY;
        var result = identity.copy(); result.setAmount((int) (units / scale)); return result;
    }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { checkTank(tank); return !stack.isEmpty(); }
    @Override public final int fill(FluidStack resource, FluidAction action) {
        long wholeSpace = (capacity * scale - units) / scale;
        long requested = Math.min(Math.max(0, resource.getAmount()), wholeSpace) * scale;
        return (int) (fillUnits(resource, requested, action) / scale);
    }
    @Override public final FluidStack drain(FluidStack resource, FluidAction action) {
        return !resource.isEmpty() && identity.isFluidEqual(resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
    }
    @Override public final FluidStack drain(int amount, FluidAction action) {
        int available = (int) Math.min(Math.max(0, amount), units / scale);
        if (available == 0) return FluidStack.EMPTY;
        var result = identity.copy(); result.setAmount(available); drainUnits(available * scale, action); return result;
    }
    public final CompoundTag save() {
        var tag = new CompoundTag(); tag.put("Fluid", identity.writeToNBT(new CompoundTag()));
        tag.putLong("Units", units); tag.putLong("Scale", scale); return tag;
    }
    public final void restore(CompoundTag tag) {
        long saved = tag.getLong("Units"); var fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("Fluid"));
        if (tag.getLong("Scale") != scale || saved < 0 || saved > capacity * scale || saved > 0 && (fluid.isEmpty() || !isFluidValid(0, fluid)))
            throw new IllegalArgumentException("Invalid tank data; explicit migration required");
        units = saved; identity = saved == 0 ? FluidStack.EMPTY : fluid.copy();
        if (!identity.isEmpty()) identity.setAmount(1);
    }
}
