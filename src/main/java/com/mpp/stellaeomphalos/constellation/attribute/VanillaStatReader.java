package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;

/** 原版直读：展示玩家原版 AttributeInstance 的当前值。 */
public class VanillaStatReader extends BoonStatReader {

    private final VanillaBoonAttribute owner;

    public VanillaStatReader(VanillaBoonAttribute owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public BoonStatLine display(Player player) {
        var targets = owner.vanillaAttributes();
        double value = baseline();
        if (!targets.isEmpty()) {
            var instance = player.getAttribute(targets.get(0));
            if (instance != null) value = instance.getValue();
        }
        return format(value);
    }

    @Override
    public BoonStatLine format(double value) {
        return new BoonStatLine(attribute, nameKey(), value, "", "");
    }
}
