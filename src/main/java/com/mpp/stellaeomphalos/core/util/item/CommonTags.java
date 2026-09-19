package com.mpp.stellaeomphalos.core.util.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class CommonTags {
    public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));
    public static final TagKey<Item> LUMEN_MATERIALS = TagKey.create(Registries.ITEM, new ResourceLocation("stellaeomphalos", "lumen_materials"));
    public static final TagKey<Fluid> MOLTEN_LUMEN = TagKey.create(Registries.FLUID, new ResourceLocation("stellaeomphalos", "molten_lumen"));
    public static final TagKey<Block> SIGHTLINE_TRANSPARENT = TagKey.create(Registries.BLOCK, new ResourceLocation("stellaeomphalos", "sightline_transparent"));
    private CommonTags() {}
}
