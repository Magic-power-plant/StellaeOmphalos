package com.mpp.stellaeomphalos.content.entity.p6;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;

/**
 * 飞行生物装配工具：微光精灵与流光液滴共用同一套飞行寻路配置。
 *
 * <p>{@code Mob#moveControl} 是 protected 字段，无法由外部工具类赋值；两个实体都在自己的构造函数里
 * 完成 {@code FlyingMoveControl} 的安装，本类只负责寻路与重力的公共部分。
 */
public final class FlyerKit {

    private FlyerKit() {}

    /** 关闭重力，交由飞控负责垂直位移。 */
    public static void makeFlyer(Mob mob) {
        mob.setNoGravity(true);
    }

    /** @return 允许漂浮的飞行寻路组件 */
    public static FlyingPathNavigation flyingNavigation(Mob mob, Level level) {
        var navigation = new FlyingPathNavigation(mob, level);
        navigation.setCanFloat(true);
        return navigation;
    }
}
