package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.constellation.attribute.ToolCrystalAttunement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 充能水晶工具族（Part-6 §6.2.2.3 / §6.4.4）。
 *
 * <p>器相（`ToolTraits`）即耐久，磨损阈值与普通工具族一致；额外维护 `ChCount` 回退计数，
 * 计数达到 {@link #REVERT_START} 后每 tick 以 1/{@link #REVERT_CHANCE} 的概率退化为惰性变体。
 * 范围破坏/探矿的异步部分改为**受控同步分片**（§6.3.6：1.12.2 的裸线程不复刻）。
 */
public final class ChargedCrystalTools {

    /** 回退计数阈值。 */
    public static final int REVERT_START = 40;

    /** 每 tick 的回退概率分母。 */
    public static final int REVERT_CHANCE = 80;

    /** 一次范围破坏的格数上限。 */
    public static final int MAX_RADIUS_BREAK = 100;

    private static final String CH_COUNT = "ChCount";
    private static final String TOOL_TRAITS = "ToolTraits";

    private ChargedCrystalTools() {}

    /** 回退状态机：由 {@code Item#inventoryTick} 驱动，只在服务端推进。 */
    public static void tickRevert(ItemStack stack, Level level, Player player) {
        if (level.isClientSide || player == null) return;
        var tag = stack.getOrCreateTag();
        if (!tag.contains(TOOL_TRAITS)) return;
        int count = tag.getInt(CH_COUNT);
        if (count < REVERT_START) return;
        if (level.random.nextInt(REVERT_CHANCE) != 0) return;
        tag.remove(CH_COUNT);
        tag.remove(TOOL_TRAITS);
        stack.setCount(1);
    }

    /** 记录一次充能使用，推进回退计数。 */
    public static void charge(ItemStack stack, Player player) {
        var tag = stack.getOrCreateTag();
        tag.putInt(CH_COUNT, Math.min(Short.MAX_VALUE, tag.getInt(CH_COUNT) + 1));
        player.getCooldowns().addCooldown(stack.getItem(), 0);
    }

    private static float attunedSpeed(ItemStack stack, float vanilla) {
        if (vanilla <= 1 || !stack.hasTag() || !stack.getTag().contains(TOOL_TRAITS)) return vanilla;
        var traits = stack.getTag().getCompound(TOOL_TRAITS);
        var quality =
                new ToolCrystalAttunement(
                        traits.getInt("Size"),
                        traits.getInt("Purity"),
                        traits.getInt("Collect"),
                        traits.getInt("Fracture"));
        return (float) (vanilla * quality.efficiency());
    }

    /** 充能斧：一次破坏整棵树的入口（分片受控）。 */
    public static final class Axe extends AxeItem implements CrystalTools.Attuned {
        public Axe() {
            super(Tiers.DIAMOND, 6, -3.2F, new net.minecraft.world.item.Item.Properties().stacksTo(1));
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return attunedSpeed(stack, super.getDestroySpeed(stack, state));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
            if (entity instanceof Player player) tickRevert(stack, level, player);
        }
    }

    /** 充能镐：右键发起受控分片探矿（半径 14）。 */
    public static final class Pickaxe extends PickaxeItem implements CrystalTools.Attuned {

        /** 探矿半径（格）。 */
        public static final int SCAN_RADIUS = 14;

        /** 单次探矿的扫描预算上限（格数 = 预算 × 步长）。 */
        public static final int SCAN_BUDGET = 8192;

        public Pickaxe() {
            super(Tiers.DIAMOND, 2, -2.8F, new net.minecraft.world.item.Item.Properties().stacksTo(1));
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return attunedSpeed(stack, super.getDestroySpeed(stack, state));
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                Level level, Player player, net.minecraft.world.InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer server) {
                // §6.2.2.3 / §6.3.6：1.12.2 的裸线程改为**受控分片**扫描，单次有硬预算，
                // 不做无界世界读写；结果消费方（高亮结果包）归 Part-8。
                var center = player.blockPosition();
                var scanner =
                        new com.mpp.stellaeomphalos.core.util.world.OreColumnScanner(
                                center,
                                SCAN_RADIUS,
                                Math.max(level.getMinBuildHeight(), center.getY() - 24),
                                Math.min(level.getMaxBuildHeight() - 1, center.getY() + 24),
                                net.minecraftforge.common.Tags.Blocks.ORES);
                int inspected = 0;
                while (inspected < SCAN_BUDGET && !scanner.advance(level, 256)) inspected += 256;
                charge(stack, player);
                player.getCooldowns().addCooldown(stack.getItem(), 60);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
            if (entity instanceof Player player) tickRevert(stack, level, player);
        }
    }

    /** 充能锹：范围挖掘同状态方块（半径 8，上限 100）。 */
    public static final class Shovel extends ShovelItem implements CrystalTools.Attuned {
        public Shovel() {
            super(Tiers.DIAMOND, 2, -3.0F, new net.minecraft.world.item.Item.Properties().stacksTo(1));
        }

        @Override
        public float getDestroySpeed(ItemStack stack, BlockState state) {
            return attunedSpeed(stack, super.getDestroySpeed(stack, state));
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
            if (entity instanceof Player player) tickRevert(stack, level, player);
        }
    }

    /** 充能剑：命中附加星陨斩；潜行时不触发。 */
    public static final class Sword extends SwordItem implements CrystalTools.Attuned {
        public Sword() {
            super(Tiers.DIAMOND, 4, -2.4F, new net.minecraft.world.item.Item.Properties().stacksTo(1));
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean result = super.hurtEnemy(stack, target, attacker);
            if (!attacker.level().isClientSide
                    && attacker instanceof Player player
                    && !player.isShiftKeyDown()
                    && !player.getCooldowns().isOnCooldown(stack.getItem())) {
                target.hurt(player.damageSources().magic(), 5.0F);
                charge(stack, player);
                player.getCooldowns().addCooldown(stack.getItem(), 80);
            }
            return result;
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
            if (entity instanceof Player player) tickRevert(stack, level, player);
        }
    }

    /** 供测试断言：位置是否在允许的探测半径内。 */
    public static boolean withinScan(BlockPos origin, BlockPos candidate, int radius) {
        return origin.distSqr(candidate) <= (double) radius * radius;
    }
}
