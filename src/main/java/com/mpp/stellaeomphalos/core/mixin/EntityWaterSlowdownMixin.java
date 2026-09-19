package com.mpp.stellaeomphalos.core.mixin;

import com.mpp.stellaeomphalos.core.platform.GameplayQueries;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EntityWaterSlowdownMixin {
    @Inject(method = "getWaterSlowDown", at = @At("RETURN"), cancellable = true, require = 1, expect = 1)
    private void querySlowdown(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(GameplayQueries.waterSlowdown((LivingEntity) (Object) this, callback.getReturnValue()));
    }
}
