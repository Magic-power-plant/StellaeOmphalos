package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;

/** 挖掘速度平值：倍率原样展示（如 1.5）。 */
public class HarvestSpeedStatReader extends BoonStatReader {

    public HarvestSpeedStatReader(BoonAttribute attribute) {
        super(attribute);
    }

    @Override
    public BoonStatLine display(Player player) {
        return format(BoonValueBridge.value(player, attribute));
    }

    @Override
    public BoonStatLine format(double value) {
        return new BoonStatLine(attribute, nameKey(), value, "x", "boon_attribute.stellaeomphalos.harvest_speed.note");
    }
}
