package com.mpp.stellaeomphalos.lumen.fluid;

import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;

public class MoltenLumenFluidType extends FluidType {
    private static volatile IClientFluidTypeExtensions clientExtensions;

    public MoltenLumenFluidType() {
        super(FluidType.Properties.create()
                .descriptionId("fluid.stellaeomphalos.molten_lumen")
                .rarity(Rarity.EPIC)
                .lightLevel(15)
                .density(1001)
                .viscosity(300)
                .temperature(120)
                // [待验证] 1.20.1 FluidType 行为矩阵需实测：canSwim(false) 期望阻止无限游泳，canDrown(true) 期望正常消耗空气
                .canSwim(false)
                .canDrown(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
    }

    /** Wired from client/render during client setup; lumen must not reference client classes at compile time. */
    public static void setClientExtensions(IClientFluidTypeExtensions extensions) { clientExtensions = extensions; }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        IClientFluidTypeExtensions extensions = clientExtensions;
        if (extensions != null) consumer.accept(extensions);
    }
}
