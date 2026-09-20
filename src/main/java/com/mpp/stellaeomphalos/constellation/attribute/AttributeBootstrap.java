package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/** 装配钩子：由集成人在 Omphalos 构造器追加 AttributeBootstrap.attach(modBus)。 */
public final class AttributeBootstrap {

    private static boolean attached;

    private AttributeBootstrap() {}

    public static synchronized void attach(IEventBus modBus) {
        if (attached) return;
        attached = true;
        BoonAttributes.init();
        var bridged = VanillaBoonAttribute.registerAll();
        VanillaBoonBridge.init(bridged);
        registerReaders(bridged);
        var forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.register(BoonAttributeListeners.class);
        forgeBus.register(VanillaBoonBridge.class);
    }

    private static void registerReaders(java.util.List<VanillaBoonAttribute> bridged) {
        bridged.forEach(owner -> BoonStatReaderRegistry.register(new VanillaStatReader(owner)));
        BoonStatReaderRegistry.register(new AdditivePercentStatReader(BoonAttributes.ELEMENTAL_WARD));
        BoonStatReaderRegistry.register(new MultiplicativePercentStatReader(BoonAttributes.PROJECTILE_VELOCITY));
        BoonStatReaderRegistry.register(new HarvestSpeedStatReader(BoonAttributes.HARVEST_SPEED));
        BoonStatReaderRegistry.register(new FlatStatReader(BoonAttributes.CRIT_CHANCE, "%"));
        BoonStatReaderRegistry.register(new MultiplicativePercentStatReader(BoonAttributes.CRIT_DAMAGE));
        BoonStatReaderRegistry.register(new AdditivePercentStatReader(BoonAttributes.DODGE));
        BoonStatReaderRegistry.register(new MultiplicativePercentStatReader(BoonAttributes.DYNAMIC_ENCHANT));
        BoonStatReaderRegistry.register(new AdditivePercentStatReader(BoonAttributes.LIFE_LEECH));
        BoonStatReaderRegistry.register(new MultiplicativePercentStatReader(BoonAttributes.LIFE_RECOVERY));
        BoonStatReaderRegistry.register(new MultiplicativePercentStatReader(BoonAttributes.BENEFICIAL_DURATION));
        BoonStatReaderRegistry.register(new MultiplicativePercentStatReader(BoonAttributes.PROJECTILE_DAMAGE));
        BoonStatReaderRegistry.register(new AdditivePercentStatReader(BoonAttributes.THORNS));
        BoonStatReaderRegistry.register(new FlatStatReader(BoonAttributes.THORNS_RANGED));
        BoonStatReaderRegistry.register(new MultiplicativePercentStatReader(BoonAttributes.BOON_POTENCY));
    }
}
