package com.mpp.stellaeomphalos.content.entity.catalog;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModEntities;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * 《方块物品实体完整清单》实体注册目录：14 个实体类型的声明与 attach。
 *
 * <p>与 {@code WorldContent} 相同的约定：所有 {@code EntityType} 在这里声明，静态初始化阶段不做任何
 * 跨注册表查询；id 形如 {@code stellaeomphalos:highlighted_item}。玩家无法通过刷怪蛋获得它们，因此
 * 一律不注册 spawn egg。
 */
public final class CatalogEntities {

    /* ---------------- 掉落物类：64 格追踪、20 刻更新 ---------------- */

    /** {@code highlighted_item}：带同步颜色的掉落物。 */
    public static final RegistrationGuard<EntityType<HighlightedItemEntity>> HIGHLIGHTED_ITEM =
            declare("highlighted_item",
                    HighlightedItemEntity::new, MobCategory.MISC, 0.25F, 0.25F, 64, 20);

    /** {@code explosion_proof_item}：免疫爆炸伤害的掉落物。 */
    public static final RegistrationGuard<EntityType<ExplosionProofItemEntity>> EXPLOSION_PROOF_ITEM =
            declare("explosion_proof_item",
                    ExplosionProofItemEntity::new, MobCategory.MISC, 0.25F, 0.25F, 64, 20);

    /** {@code star_dust}：在熔融流光上累积惰性合并的星尘。 */
    public static final RegistrationGuard<EntityType<StarDustEntity>> STAR_DUST =
            declare("star_dust", StarDustEntity::new, MobCategory.MISC, 0.25F, 0.25F, 64, 20);

    /** {@code geode_entity}：只在流光上"生长"的晶簇掉落物。 */
    public static final RegistrationGuard<EntityType<GeodeEntity>> GEODE_ENTITY =
            declare("geode_entity", GeodeEntity::new, MobCategory.MISC, 0.25F, 0.25F, 64, 20);

    /** {@code geode_tool_entity}：可设为永不消失的晶体工具掉落物。 */
    public static final RegistrationGuard<EntityType<GeodeToolEntity>> GEODE_TOOL_ENTITY =
            declare("geode_tool_entity", GeodeToolEntity::new, MobCategory.MISC, 0.5F, 0.5F, 64, 20);

    /* ---------------- 投射物类 ---------------- */

    /** {@code grapnel}：命中后把主人拉向钩点的抓钩。 */
    public static final RegistrationGuard<EntityType<GrapnelEntity>> GRAPNEL =
            declare("grapnel", GrapnelEntity::new, MobCategory.MISC, 0.3F, 0.3F, 64, 1);

    /** {@code lucent_spark}：落地放置易逝光源的辉光火花。 */
    public static final RegistrationGuard<EntityType<LucentSparkEntity>> LUCENT_SPARK =
            declare("lucent_spark", LucentSparkEntity::new, MobCategory.MISC, 0.25F, 0.25F, 32, 1);

    /** {@code umbral_spark}：孵化并尝试召唤怪物的幽影火花。 */
    public static final RegistrationGuard<EntityType<UmbralSparkEntity>> UMBRAL_SPARK =
            declare("umbral_spark", UmbralSparkEntity::new, MobCategory.MISC, 0.25F, 0.25F, 32, 1);

    /** {@code star_bolt}：向服务端目标归航的星矢。 */
    public static final RegistrationGuard<EntityType<StarBoltEntity>> STAR_BOLT =
            declare("star_bolt", StarBoltEntity::new, MobCategory.MISC, 0.25F, 0.25F, 32, 1);

    /** {@code falling_star}：沿固定水平矢量飞行、无破坏落地的流星。 */
    public static final RegistrationGuard<EntityType<FallingStarEntity>> FALLING_STAR =
            declare("falling_star", FallingStarEntity::new, MobCategory.MISC, 0.5F, 0.5F, 128, 1);

    /* ---------------- 功能性实体 ---------------- */

    /** {@code observatory_seat}：零体积观测台座椅，所属方块消失即自毁。 */
    public static final RegistrationGuard<EntityType<ObservatorySeatEntity>> OBSERVATORY_SEAT =
            declare("observatory_seat", ObservatorySeatEntity::new, MobCategory.MISC, 0.0F, 0.0F, 64, 1);

    /** {@code wisp}：环境氛围飞行生物。 */
    public static final RegistrationGuard<EntityType<WispEntity>> WISP =
            declare("wisp", WispEntity::new, MobCategory.AMBIENT, 0.5F, 0.5F, 64, 20);

    /** {@code lumen_droplet}：携带流体、飞向目标的液体载具。 */
    public static final RegistrationGuard<EntityType<LumenDropletEntity>> LUMEN_DROPLET =
            declare("lumen_droplet", LumenDropletEntity::new, MobCategory.AMBIENT, 0.5F, 0.5F, 64, 1);

    /** {@code phantom_tool}：按任务执行有限作业的工具实体。 */
    public static final RegistrationGuard<EntityType<PhantomToolEntity>> PHANTOM_TOOL =
            declare("phantom_tool", PhantomToolEntity::new, MobCategory.MISC, 0.5F, 0.5F, 64, 20);

    private CatalogEntities() {}

    /**
     * 只做"确保本类已初始化"：上面的静态声明必须赶在 {@code RegistryBootstrap} 挂载注册表之前完成。
     *
     * <p>实体注册表由 {@code RegistryBootstrap} 统一 attach，这里不重复调用（{@code RegistryFamily}
     * 会拒绝重复挂载）。
     */
    public static void attach(IEventBus bus) {
        CatalogEntities.class.getName();
        bus.addListener(CatalogEntities::attributes);
    }

    private static <T extends net.minecraft.world.entity.LivingEntity> EntityType<T> attributeType(RegistrationGuard<EntityType<T>> guard) {
        var type=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(guard.id());
        if(type==null)throw new IllegalStateException("Missing living entity type "+guard.id());
        @SuppressWarnings("unchecked") var checked=(EntityType<T>)type;
        return checked;
    }

    private static void attributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(attributeType(WISP), WispEntity.createAttributes().build());
        event.put(attributeType(LUMEN_DROPLET), LumenDropletEntity.createAttributes().build());
        event.put(attributeType(PHANTOM_TOOL), PhantomToolEntity.createAttributes().build());
    }

    /** 统一构造 {@code EntityType.Builder}，避免逐个重复追踪参数。 */
    private static <T extends Entity> RegistrationGuard<EntityType<T>> declare(
            String id,
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            float width,
            float height,
            int trackingRange,
            int updateInterval) {
        return ModEntities.ENTRIES.declare(id, () -> build(id, factory, category, width, height,
                trackingRange, updateInterval));
    }

    private static <T extends Entity> EntityType<T> build(
            String id,
            EntityType.EntityFactory<T> factory,
            MobCategory category,
            float width,
            float height,
            int trackingRange,
            int updateInterval) {
        return EntityType.Builder.of(factory, category)
                .sized(width, height)
                .clientTrackingRange(trackingRange)
                .updateInterval(updateInterval)
                .build("stellaeomphalos:" + id);
    }
}
