package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 掉落物实体替换（《方块物品实体完整清单》§6.6.2 的最后三行）。
 *
 * <p>“任意 {@link HighlightedItem} 掉落 → `highlighted_item`”、“水晶/宝石/工具掉落 → `geode_entity` /
 * `geode_tool_entity`”、“星屑 → `star_dust`”在 1.20.1 没有物品钩子可用，因此改为在实体加入世界时
 * **替换实体类型**：取消原版 `ItemEntity` 的加入，生成对应的专用实体并继承位置、动量与物品栈。
 *
 * <p>替换是**幂等**的：专用实体本身也是 `ItemEntity` 的子类，因此监听器首先排除己方类型，避免自触发。
 */
public final class ItemEntityReplacement {

    private ItemEntityReplacement() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        // 己方类型直接放行（幂等守卫）。
        if (isOwnType(item)) return;
        var type = replacementFor(item.getItem());
        if (type == null) return;
        var replacement = type.create(event.getLevel());
        if (replacement == null) return;
        replacement.setPos(item.getX(), item.getY(), item.getZ());
        replacement.setDeltaMovement(item.getDeltaMovement());
        replacement.setItem(item.getItem().copy());
        replacement.setPickUpDelay(item.hasPickUpDelay() ? 10 : 10);
        event.setCanceled(true);
        event.getLevel().addFreshEntity(replacement);
    }

    /** 是否已经是本模组的专用掉落物类型。 */
    static boolean isOwnType(ItemEntity item) {
        return item
                        instanceof
                        com.mpp.stellaeomphalos.content.entity.catalog.HighlightedItemEntity
                || item
                        instanceof
                        com.mpp.stellaeomphalos.content.entity.catalog.ExplosionProofItemEntity
                || item instanceof com.mpp.stellaeomphalos.content.entity.catalog.StarDustEntity
                || item instanceof com.mpp.stellaeomphalos.content.entity.catalog.GeodeEntity
                || item instanceof com.mpp.stellaeomphalos.content.entity.catalog.GeodeToolEntity;
    }

    /**
     * 给定物品栈应当使用的专用实体类型；不需要替换时返回 null。
     *
     * <p>判定顺序：高亮 → 抗爆 → 星屑 → 工具 → 水晶/宝石。
     */
    public static EntityType<? extends ItemEntity> replacementFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var item = stack.getItem();
        if (item instanceof HighlightedItem) return CatalogEntities.HIGHLIGHTED_ITEM.get();
        if (hasId(item, "lore_capsule")) return CatalogEntities.EXPLOSION_PROOF_ITEM.get();
        if (isStarDust(item)) return CatalogEntities.STAR_DUST.get();
        if (CatalogItems.isCrystalTool(item)) return CatalogEntities.GEODE_TOOL_ENTITY.get();
        if (CatalogItems.isCrystalOrGem(item)) return CatalogEntities.GEODE_ENTITY.get();
        return null;
    }

    /** 星屑：注册 id 为 `star_dust`。 */
    private static boolean isStarDust(net.minecraft.world.item.Item item) {
        return hasId(item, "star_dust");
    }

    /** 按命名空间 + 路径判定物品（不走注册守卫，避免加载期访问）。 */
    private static boolean hasId(net.minecraft.world.item.Item item, String path) {
        if (item == null) return false;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        return id != null
                && id.getNamespace().equals(com.mpp.stellaeomphalos.Omphalos.MODID)
                && id.getPath().equals(path);
    }

    /** 供玩家调试命令使用：当前玩家前方的掉落物是否会走替换路径。 */
    public static boolean wouldReplace(ItemStack stack) {
        return replacementFor(stack) != null;
    }

    /** 供测试构造一个"由某玩家丢出"的掉落物。 */
    public static ItemEntity dropFrom(Player player, ItemStack stack) {
        var entity =
                new ItemEntity(
                        player.level(),
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        stack.copy());
        entity.setPickUpDelay(40);
        return entity;
    }
}
