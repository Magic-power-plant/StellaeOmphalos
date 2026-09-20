package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;

/** 展示数据 / 默认基线 / 指定模式累计修量 三契约；format 为纯逻辑，display 经桥取值。 */
public abstract class BoonStatReader {

    protected final BoonAttribute attribute;

    protected BoonStatReader(BoonAttribute attribute) {
        if (attribute == null) throw new IllegalArgumentException("null attribute");
        this.attribute = attribute;
    }

    public final BoonAttribute attribute() {
        return attribute;
    }

    public double baseline() {
        return attribute.defaultValue();
    }

    public double accumulated(Player player, BoonModifier.Mode mode) {
        return BoonValueBridge.resolve(player, attribute, mode);
    }

    public abstract BoonStatLine display(Player player);

    public abstract BoonStatLine format(double value);

    protected final String nameKey() {
        return "boon_attribute." + attribute.id().getNamespace() + "." + attribute.id().getPath();
    }
}
