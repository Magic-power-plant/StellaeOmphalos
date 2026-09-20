package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;

/** 乘法百分比：倍率相对 1.0 的增量 ×100 展示（如暴击伤害 1.5 → +50%）。 */
public class MultiplicativePercentStatReader extends BoonStatReader {

    public MultiplicativePercentStatReader(BoonAttribute attribute) {
        super(attribute);
    }

    @Override
    public BoonStatLine display(Player player) {
        return format(BoonValueBridge.value(player, attribute));
    }

    @Override
    public BoonStatLine format(double value) {
        return new BoonStatLine(attribute, nameKey(), (value - 1.0) * 100.0, "%", "");
    }
}
