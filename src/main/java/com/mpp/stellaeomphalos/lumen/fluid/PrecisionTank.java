package com.mpp.stellaeomphalos.lumen.fluid;

import net.minecraftforge.fluids.FluidStack;

/** One million subunits per mB avoids floating-point accumulation drift. */
public final class PrecisionTank extends FluidStorage {
    public PrecisionTank(int capacity, Runnable changed) { super(capacity, 1000000, changed); }
    public long fillSubunits(FluidStack fluid, long subunits, FluidAction action) { return fillUnits(fluid, subunits, action); }
    public long drainSubunits(long subunits, FluidAction action) { return drainUnits(subunits, action); }
}
