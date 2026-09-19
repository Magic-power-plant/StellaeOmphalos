package com.mpp.stellaeomphalos.lumen.fluid;

public final class SingleTank extends FluidStorage {
    public SingleTank(int capacity, Runnable changed) { super(capacity, 1, changed); }
}
