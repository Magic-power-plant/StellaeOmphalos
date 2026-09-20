package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class BoonDamageTypes {

    private static final String NAMESPACE = "stellaeomphalos";

    /** 荆棘反弹专属伤害类型，监听侧据此跳过防递归；数据驱动 JSON 在 data/stellaeomphalos/damage_type/。 */
    public static final ResourceKey<DamageType> BOON_THORNS =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(NAMESPACE, "boon_thorns"));

    /** 元素伤害类型关键词表（TagKey 驱动），JSON 在 data/stellaeomphalos/tags/damage_type/elemental.json。 */
    public static final TagKey<DamageType> ELEMENTAL =
            TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(NAMESPACE, "elemental"));

    private BoonDamageTypes() {}
}
