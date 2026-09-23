package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

/** 动态附魔钩子：附魔体系（《星坛与制作系统》/《星典知识与玩家进度》）在追加附魔等级前投递本事件，属性模块按 dynamic_enchant 倍率 round 到整级。 */
public class DynamicEnchantEvent extends Event {

    private final Player player;
    private final double baseLevel;
    private int adjustedLevel;

    public DynamicEnchantEvent(Player player, double baseLevel) {
        this.player = player;
        this.baseLevel = baseLevel;
        this.adjustedLevel = (int) Math.round(baseLevel);
    }

    public Player player() {
        return player;
    }

    public double baseLevel() {
        return baseLevel;
    }

    public int adjustedLevel() {
        return adjustedLevel;
    }

    public void setAdjustedLevel(int adjustedLevel) {
        this.adjustedLevel = adjustedLevel;
    }
}
