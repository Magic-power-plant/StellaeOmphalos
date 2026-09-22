package com.mpp.stellaeomphalos.constellation.starmap;

import com.mpp.stellaeomphalos.constellation.sign.SignDiscoveryView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * Immutable result of starmap engraving: sign proportions plus the drawn points for GUI echo.
 * Lives on the item under the "Imprint" NBT tag (PascalCase keys, structure "Version").
 */
public record SignImprint(Map<ResourceLocation, Double> proportions, Map<ResourceLocation, List<StarPoint>> points) {
    public static final String TAG = "Imprint";
    public static final int STRUCTURE_VERSION = 1;

    public SignImprint {
        proportions = Map.copyOf(proportions);
        var copy = new LinkedHashMap<ResourceLocation, List<StarPoint>>();
        points.forEach((sign, list) -> copy.put(sign, List.copyOf(list)));
        points = Map.copyOf(copy);
    }

    /** Applies sign enchantments; signs the player has not discovered yield nothing. */
    public void applyEnchantments(ItemStack stack, RandomSource random, SignDiscoveryView discovery) {
        boolean book = stack.is(Items.ENCHANTED_BOOK);
        Map<Enchantment, Integer> existing = book
                ? EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack))
                : EnchantmentHelper.getEnchantments(stack);
        for (var entry : proportions.entrySet()) {
            if (!discovery.knowsSign(entry.getKey())) continue;
            var candidates = SignImprintEffectRegistry.enchantments(entry.getKey());
            var chosen = SignImprintEffectRegistry.selectApplicable(candidates,
                    affix -> book ? affix.enchantment().value().isAllowedOnBooks()
                            : affix.enchantment().value().canApplyAtEnchantingTable(stack),
                    (affix, accepted) -> affix.ignoreCompatibility() || affix.enchantment().value().isCompatibleWith(accepted),
                    affix -> affix.enchantment().value(),
                    existing.keySet());
            for (var affix : chosen) {
                int level = SignImprintEffectRegistry.levelFor(affix.minLevel(), affix.maxLevel(), entry.getValue());
                if (book) EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(affix.enchantment().value(), level));
                else stack.enchant(affix.enchantment().value(), level);
            }
        }
    }

    /**
     * Stores imprint mob effects on the item ("Effects" list under the imprint tag): duration
     * 4800 + random(0..2400) ticks, same-effect entries keep the higher level, and with 1/30
     * probability the death-protection effect is appended.
     */
    public void applyMobEffects(ItemStack stack, RandomSource random) {
        record Pending(MobEffect effect, int amplifier, int duration) {}
        var merged = new LinkedHashMap<MobEffect, Pending>();
        for (var entry : proportions.entrySet())
            for (var potion : SignImprintEffectRegistry.potions(entry.getKey())) {
                int level = SignImprintEffectRegistry.levelFor(potion.minLevel(), potion.maxLevel(), entry.getValue());
                var pending = new Pending(potion.effect().value(), level, 4800 + random.nextInt(2401));
                merged.merge(potion.effect().value(), pending,
                        (a, b) -> a.amplifier() >= b.amplifier() ? a : b);
            }
        if (random.nextInt(30) == 0)
            merged.merge(StarmapContent.DEATH_PROTECTION.get(), new Pending(StarmapContent.DEATH_PROTECTION.get(), 0, 4800),
                    (a, b) -> a.amplifier() >= b.amplifier() ? a : b);
        stack.getOrCreateTagElement(TAG).remove("Effects");
        if (merged.isEmpty()) return;
        var list = new ListTag();
        for (var pending : merged.values()) {
            var tag = new CompoundTag();
            tag.putString("Id", BuiltInRegistries.MOB_EFFECT.getKey(pending.effect()).toString());
            tag.putInt("Amplifier", pending.amplifier());
            tag.putInt("Duration", pending.duration());
            list.add(tag);
        }
        stack.getOrCreateTagElement(TAG).put("Effects", list);
    }

    /** Consume the stored dose once; re-equipping cannot refresh it or restore cheat death. */
    public static boolean activateEffects(ItemStack stack, net.minecraft.world.entity.LivingEntity wearer) {
        if (wearer.level().isClientSide || read(stack).isEmpty()) return false;
        var tag = stack.getTagElement(TAG);
        if (tag == null || !tag.contains("Effects", Tag.TAG_LIST)) return false;
        var effects = tag.getList("Effects", Tag.TAG_COMPOUND).copy();
        tag.remove("Effects");
        for (int i = 0; i < Math.min(64, effects.size()); i++) {
            var entry = effects.getCompound(i);
            var id = ResourceLocation.tryParse(entry.getString("Id"));
            var effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
            if (effect == null || entry.getInt("Duration") <= 0) continue;
            wearer.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,
                    Math.min(7200, entry.getInt("Duration")), Math.max(0, Math.min(255, entry.getInt("Amplifier")))));
        }
        return true;
    }

    public void write(ItemStack stack) {
        var tag = stack.getOrCreateTagElement(TAG);
        tag.putInt("Version", STRUCTURE_VERSION);
        var stars = new ListTag();
        proportions.forEach((sign, ratio) -> {
            var entry = new CompoundTag();
            entry.putString("Sign", sign.toString());
            entry.putDouble("Ratio", ratio);
            stars.add(entry);
        });
        tag.put("Stars", stars);
        var pointSets = new ListTag();
        points.forEach((sign, list) -> {
            var entry = new CompoundTag();
            entry.putString("Sign", sign.toString());
            var flat = new int[list.size() * 2];
            for (int i = 0; i < list.size(); i++) {
                flat[i * 2] = list.get(i).x();
                flat[i * 2 + 1] = list.get(i).y();
            }
            entry.putIntArray("Pts", flat);
            pointSets.add(entry);
        });
        tag.put("Points", pointSets);
    }

    /** Entries whose sign id fails to resolve are skipped; unknown structure version yields empty. */
    public static Optional<SignImprint> read(ItemStack stack) {
        var root = stack.getTag();
        if (root == null || !root.contains(TAG, Tag.TAG_COMPOUND)) return Optional.empty();
        var tag = root.getCompound(TAG);
        if (tag.getInt("Version") != STRUCTURE_VERSION) return Optional.empty();
        var proportions = new LinkedHashMap<ResourceLocation, Double>();
        for (var element : tag.getList("Stars", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var sign = ResourceLocation.tryParse(entry.getString("Sign"));
            if (sign == null || !signKnown(sign)) continue;
            double ratio = entry.getDouble("Ratio");
            if (Double.isFinite(ratio)) proportions.put(sign, Math.max(0, Math.min(1, ratio)));
        }
        var points = new LinkedHashMap<ResourceLocation, List<StarPoint>>();
        for (var element : tag.getList("Points", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) element;
            var sign = ResourceLocation.tryParse(entry.getString("Sign"));
            if (sign == null || !signKnown(sign)) continue;
            int[] flat = entry.getIntArray("Pts");
            var list = new java.util.ArrayList<StarPoint>();
            for (int i = 0; i + 1 < flat.length && i < 4096; i += 2)
                if (flat[i] >= 0 && flat[i] < StarPoint.GRID && flat[i + 1] >= 0 && flat[i + 1] < StarPoint.GRID)
                    list.add(new StarPoint(flat[i], flat[i + 1]));
            points.put(sign, list);
        }
        return Optional.of(new SignImprint(proportions, points));
    }

    private static boolean signKnown(ResourceLocation sign) {
        return com.mpp.stellaeomphalos.constellation.sign.SignRegistry.byId(sign) != null;
    }
}
