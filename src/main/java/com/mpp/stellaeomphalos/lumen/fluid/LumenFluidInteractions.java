package com.mpp.stellaeomphalos.lumen.fluid;

import com.mpp.stellaeomphalos.OmphalosConfig;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidInteractionRegistry;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Molten lumen reactions: neighboring fluids solidify it by temperature, players inside get night
 * vision, and log item entities are infused one log at a time. Pure decision helpers are kept
 * separate from world access for unit testing.
 */
public final class LumenFluidInteractions {
    // Keys are read defensively from the SERVER snapshot with these defaults until the integrator
    // adds the definitions to OmphalosConfig.
    public static final String CONFIG_HOT_MODE = "gameplay.lumenFluidHotInteraction";
    public static final String CONFIG_COLD_RESULT = "gameplay.lumenFluidColdResultBlock";
    public static final String CONFIG_HOT_RESULT = "gameplay.lumenFluidHotResultBlock";
    public static final int NIGHT_VISION_DURATION = 300;
    public static final int NIGHT_VISION_REFRESH_THRESHOLD = 260;
    public static final int GEODE_SHARD_DENOMINATOR = 900;
    private static final String DEFAULT_COLD_RESULT = "minecraft:ice";
    private static final String DEFAULT_HOT_RESULT = "minecraft:sand";

    private LumenFluidInteractions() {}

    public enum HotInteractionMode {
        OFF, SAND, SAND_AND_RARE;

        public static HotInteractionMode parse(@Nullable String raw) {
            if (raw == null) return SAND_AND_RARE;
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "off" -> OFF;
                case "sand" -> SAND;
                case "sand_and_rare" -> SAND_AND_RARE;
                default -> SAND_AND_RARE;
            };
        }
    }

    public record LogConversion(int remaining, int produced) {}

    public static boolean isColdReaction(int neighborTemperature) {
        return neighborTemperature <= LumenFluidThermal.COLD_THRESHOLD;
    }

    /** @return result block id, or null when the hot interaction is disabled or the id is invalid */
    @Nullable
    public static ResourceLocation reactionResultId(int neighborTemperature, HotInteractionMode mode, String coldId, String hotId) {
        if (isColdReaction(neighborTemperature)) return ResourceLocation.tryParse(coldId);
        if (mode == HotInteractionMode.OFF) return null;
        return ResourceLocation.tryParse(hotId);
    }

    /** @param roll outcome of {@code random.nextInt(GEODE_SHARD_DENOMINATOR)} */
    public static boolean rollsRareDrop(int roll) { return roll == 0; }

    public static LogConversion logConversion(int stackCount) {
        return stackCount > 0 ? new LogConversion(stackCount - 1, 1) : new LogConversion(0, 0);
    }

    /**
     * Interaction registered against our own FluidType: LiquidBlock invokes it from onPlace and
     * neighborChanged with currentPos = the molten lumen block, so the lumen block is what
     * solidifies. (1.20.1 Forge has no fluid interaction event; shouldSpreadLiquid is private.)
     */
    public static FluidInteractionRegistry.InteractionInformation interactionInfo() {
        return new FluidInteractionRegistry.InteractionInformation(
                (level, currentPos, relativePos, currentState) -> {
                    FluidState neighbor = level.getFluidState(relativePos);
                    return !neighbor.isEmpty() && neighbor.getFluidType() != currentState.getFluidType();
                },
                LumenFluidInteractions::react);
    }

    private static void react(Level level, BlockPos currentPos, BlockPos relativePos, FluidState currentState) {
        int temperature = LumenFluidThermal.temperatureOf(level.getFluidState(relativePos));
        HotInteractionMode mode = hotMode();
        ResourceLocation resultId = reactionResultId(temperature, mode,
                configString(CONFIG_COLD_RESULT, DEFAULT_COLD_RESULT), configString(CONFIG_HOT_RESULT, DEFAULT_HOT_RESULT));
        if (resultId == null) return;
        Block fallback = isColdReaction(temperature) ? Blocks.ICE : Blocks.SAND;
        Block result = ForgeRegistries.BLOCKS.containsKey(resultId) ? ForgeRegistries.BLOCKS.getValue(resultId) : fallback;
        level.setBlockAndUpdate(currentPos, result.defaultBlockState());
        level.levelEvent(1501, currentPos, 0);
        if (!isColdReaction(temperature) && mode == HotInteractionMode.SAND_AND_RARE && !level.isClientSide
                && rollsRareDrop(level.random.nextInt(GEODE_SHARD_DENOMINATOR))) {
            level.addFreshEntity(new ItemEntity(level, currentPos.getX() + 0.5, currentPos.getY() + 0.5, currentPos.getZ() + 0.5,
                    new ItemStack(MoltenLumenContent.GEODE_SHARD.get())));
        }
    }

    public static void onEntityInside(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (entity instanceof Player player) {
            MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
            if (existing == null || existing.getDuration() < NIGHT_VISION_REFRESH_THRESHOLD) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NIGHT_VISION_DURATION, 0, true, true));
            }
        } else if (entity instanceof ItemEntity item) {
            ItemStack stack = item.getItem();
            if (!stack.is(ItemTags.LOGS)) return;
            LogConversion conversion = logConversion(stack.getCount());
            if (conversion.produced() <= 0) return;
            ItemEntity infused = new ItemEntity(level, item.getX(), item.getY(), item.getZ(),
                    new ItemStack(MoltenLumenContent.INFUSED_LOG_ITEM.get(), conversion.produced()));
            infused.setDeltaMovement(item.getDeltaMovement());
            level.addFreshEntity(infused);
            stack.shrink(conversion.produced());
            if (stack.isEmpty()) item.discard();
            else item.setItem(stack);
        }
    }

    private static HotInteractionMode hotMode() {
        Object raw = OmphalosConfig.SERVER.snapshot().get(CONFIG_HOT_MODE);
        return HotInteractionMode.parse(raw == null ? null : raw.toString());
    }

    private static String configString(String key, String fallback) {
        Object raw = OmphalosConfig.SERVER.snapshot().get(key);
        return raw == null ? fallback : raw.toString();
    }
}
