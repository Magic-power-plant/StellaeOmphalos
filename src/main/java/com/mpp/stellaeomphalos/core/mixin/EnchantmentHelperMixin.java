package com.mpp.stellaeomphalos.core.mixin;

import com.mpp.stellaeomphalos.core.platform.GameplayQueries;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getItemEnchantmentLevel", at = @At("RETURN"), cancellable = true, require = 1, expect = 1)
    private static void queryLevel(Enchantment enchantment, ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(GameplayQueries.enchantments(stack, Map.of(enchantment, callback.getReturnValue())).getOrDefault(enchantment, 0));
    }
    @Inject(method = "getEnchantments", at = @At("RETURN"), cancellable = true, require = 1, expect = 1)
    private static void queryLevels(ItemStack stack, CallbackInfoReturnable<Map<Enchantment, Integer>> callback) {
        callback.setReturnValue(new java.util.LinkedHashMap<>(GameplayQueries.enchantments(stack, callback.getReturnValue())));
    }
}
