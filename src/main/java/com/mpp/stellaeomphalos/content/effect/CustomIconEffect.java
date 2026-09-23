package com.mpp.stellaeomphalos.content.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 《方块物品实体完整清单》药水的公共基类。该类**刻意不覆写** {@code hasStatusIcon()}：原版图集图标保留，
 * 客户端渲染工作流可另行读取 {@link #iconTexture(ResourceLocation)} 指向的 18×18 自定义贴图，
 * 顶点色取原版 {@code getColor()}（即构造时传入的颜色）自动染色，因此子类只需一张底图。
 */
public abstract class CustomIconEffect extends MobEffect {

    /** 自定义图标目录；贴图命名约定为 {@code textures/mob_effect/<effect_path>.png}。 */
    private static final String ICON_DIRECTORY = "textures/mob_effect/";

    protected CustomIconEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /**
     * 供客户端在注册期建立自定义图标绑定；服务端只做字符串拼接，不加载任何资源。
     */
    public ResourceLocation iconTexture(ResourceLocation effectId) {
        return new ResourceLocation(effectId.getNamespace(), ICON_DIRECTORY + effectId.getPath() + ".png");
    }
}
