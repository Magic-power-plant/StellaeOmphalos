package com.mpp.stellaeomphalos;

import com.mpp.stellaeomphalos.constellation.attribute.AttributeBootstrap;
import com.mpp.stellaeomphalos.constellation.domain.DomainBootstrap;
import com.mpp.stellaeomphalos.constellation.sign.SignBootstrap;
import com.mpp.stellaeomphalos.constellation.sign.SignServerHooks;
import com.mpp.stellaeomphalos.constellation.starmap.StarmapBootstrap;
import com.mpp.stellaeomphalos.core.bootstrap.RegistryBootstrap;
import com.mpp.stellaeomphalos.lumen.fluid.MoltenLumenBootstrap;
import com.mpp.stellaeomphalos.lumen.transport.LumenBootstrap;
import com.mpp.stellaeomphalos.lumen.transport.stasis.StasisBootstrap;
import com.mpp.stellaeomphalos.network.NetworkBootstrap;
import com.mpp.stellaeomphalos.player.boon.BoonBootstrap;
import com.mpp.stellaeomphalos.player.charge.ChargeBootstrap;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Omphalos.MODID)
public final class Omphalos {

    public static final String MODID = "stellaeomphalos";

    public Omphalos() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        RegistryBootstrap.attach(bus);
        MoltenLumenBootstrap.attach(bus);
        LumenBootstrap.attach(bus);
        StasisBootstrap.attach(bus);
        SignBootstrap.attach(bus);
        SignServerHooks.attachServer();
        StarmapBootstrap.attach(bus);
        DomainBootstrap.attach(bus);
        AttributeBootstrap.attach(bus);
        BoonBootstrap.attach(bus);
        ChargeBootstrap.attach(bus);
        OmphalosConfig.register(bus);
        com.mpp.stellaeomphalos.content.world.WorldBootstrap.attach(bus);
        com.mpp.stellaeomphalos.content.item.knowledge.KnowledgeBootstrap.attach(bus);
        NetworkBootstrap.attach(bus);
    }
}
