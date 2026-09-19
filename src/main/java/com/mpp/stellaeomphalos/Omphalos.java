package com.mpp.stellaeomphalos;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.mpp.stellaeomphalos.core.bootstrap.RegistryBootstrap;
import com.mpp.stellaeomphalos.network.NetworkBootstrap;

@Mod(Omphalos.MODID)
public final class Omphalos {

    public static final String MODID = "stellaeomphalos";

    public Omphalos() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        RegistryBootstrap.attach(bus);
        OmphalosConfig.register(bus);
        NetworkBootstrap.attach(bus);
    }

}
