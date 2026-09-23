package com.mpp.stellaeomphalos.content.entity.catalog;

import com.mpp.stellaeomphalos.Omphalos;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 辉光火花：命中可替换方块时放下一枚易逝光源（{@code stellaeomphalos:ephemeral_light}）后消失。
 *
 * <p>方块通过 {@link ForgeRegistries#BLOCKS} 惰性查询并做空值判断，因此即使在尚未注册该方块的
 * 数据包环境下也只是"不放置、直接消失"，绝不抛异常。光源方块自带随机刻自毁，本类不再做延迟清理。
 */
public class LucentSparkEntity extends ThrowableItemProjectile {

    private static final String LIGHT_ID = "ephemeral_light";

    public LucentSparkEntity(EntityType<? extends LucentSparkEntity> type, Level level) {
        super(type, level);
    }

    public LucentSparkEntity(EntityType<? extends LucentSparkEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.GLOWSTONE_DUST;
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (level().isClientSide) {
            discard();
            return;
        }
        placeLight(hit.getBlockPos().relative(hit.getDirection()));
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level().isClientSide) {
            discard();
            return;
        }
        placeLight(hit.getEntity().blockPosition());
        discard();
    }

    /** 在给定方块位放置光源；不可替换或方块缺失时静默跳过。 */
    private void placeLight(BlockPos pos) {
        placeLightAt(level(), pos);
    }

    /**
     * 静态入口：把易逝光源放到给定位置，供辉光粉物品与其它调用方复用（§6.6.2）。
     *
     * @return 是否成功放置
     */
    public static boolean placeLightAt(Level level, BlockPos pos) {
        Block light = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Omphalos.MODID, LIGHT_ID));
        if (light == null) return false;
        BlockState target = light.defaultBlockState();
        BlockPos immutable = pos.immutable();
        if (!level.hasChunkAt(immutable)) return false;
        BlockState existing = level.getBlockState(immutable);
        if (!existing.isAir() && !existing.canBeReplaced()) return false;
        return level.setBlockAndUpdate(immutable, target);
    }

    /** @return 光源方块是否已被注册（供外部判断可用性） */
    public static boolean lightAvailable() {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Omphalos.MODID, LIGHT_ID)) != null;
    }

    /** 供调用方在发射前检查是否有可站立的方块位。 */
    public static boolean canBeLit(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !target.isSpectator();
    }
}
