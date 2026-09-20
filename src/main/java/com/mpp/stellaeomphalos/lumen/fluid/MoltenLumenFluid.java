package com.mpp.stellaeomphalos.lumen.fluid;

import net.minecraftforge.fluids.ForgeFlowingFluid;

/** Molten lumen fluid states; the outer class is a namespace holder. */
public final class MoltenLumenFluid {
    private MoltenLumenFluid() {}

    public static final class Source extends ForgeFlowingFluid.Source {
        public Source(ForgeFlowingFluid.Properties properties) { super(properties); }
    }

    public static final class Flowing extends ForgeFlowingFluid.Flowing {
        public Flowing(ForgeFlowingFluid.Properties properties) { super(properties); }
    }
}
