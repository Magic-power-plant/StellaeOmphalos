package com.mpp.stellaeomphalos.content.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 天空共鸣器（《方块物品实体完整清单》§6.2.2.5 / §6.6.2）：三模式侦察器。
 *
 * <p>潜行右键在已解锁模式间轮换：`STARLIGHT` 无条件可用，`FLUID_FIELDS` 需要研习进度，
 * `AREA_SIZE` 需要共鸣。**越界或权限不足一律回退 `STARLIGHT`**，与规划要求的六重容错回退一致。
 * 模式在物品 NBT 的 `Mode` 键上持久化（PascalCase，遵循项目 NBT 规范）。
 */
public final class SkyResonatorItem extends Item {

    /** 模式键。 */
    public static final String MODE = "Mode";

    /** 侦察模式，顺序即轮换顺序。 */
    public enum Mode {
        /** 无条件：显示星辉场强度。 */
        STARLIGHT,
        /** 需要研习进度：显示流体分布。 */
        FLUID_FIELDS,
        /** 需要共鸣：显示作用半径预览。 */
        AREA_SIZE;

        public String translationKey() {
            return "item.stellaeomphalos.sky_resonator.mode." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public SkyResonatorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** 当前模式；NBT 缺失或非法时回退 {@link Mode#STARLIGHT}。 */
    public static Mode mode(ItemStack stack) {
        if (!stack.hasTag()) return Mode.STARLIGHT;
        var raw = stack.getTag().getString(MODE);
        for (var candidate : Mode.values()) if (candidate.name().equals(raw)) return candidate;
        return Mode.STARLIGHT;
    }

    /**
     * 权限判定入口；调用方传入"已解锁的研习支/是否已共鸣"。
     *
     * @return 实际生效的模式（越界或权限不足时回退 STARLIGHT）
     */
    public static Mode resolve(Mode requested, boolean fluidUnlocked, boolean attuned) {
        if (requested == null) return Mode.STARLIGHT;
        return switch (requested) {
            case STARLIGHT -> Mode.STARLIGHT;
            case FLUID_FIELDS -> fluidUnlocked ? Mode.FLUID_FIELDS : Mode.STARLIGHT;
            case AREA_SIZE -> attuned ? Mode.AREA_SIZE : Mode.STARLIGHT;
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                var next = Mode.values()[(mode(stack).ordinal() + 1) % Mode.values().length];
                stack.getOrCreateTag().putString(MODE, next.name());
                player.displayClientMessage(Component.translatable(next.translationKey()), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(
                Component.translatable(mode(stack).translationKey()).withStyle(ChatFormatting.AQUA));
    }
}
