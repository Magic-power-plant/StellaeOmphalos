package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;

/** 加法百分比：通道累计值 ×100 展示（如闪避 0.3 → 30%）。 */
public class AdditivePercentStatReader extends BoonStatReader {

    public AdditivePercentStatReader(BoonAttribute attribute) {
        super(attribute);
    }

    @Override
    public BoonStatLine display(Player player) {
        return format(BoonValueBridge.value(player, attribute));
    }

    @Override
    public BoonStatLine format(double value) {
        return new BoonStatLine(attribute, nameKey(), value * 100.0, "%", "");
    }
}
