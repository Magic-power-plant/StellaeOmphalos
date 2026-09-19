package com.mpp.stellaeomphalos.core.util.entity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EntityKit {
    private EntityKit() {}
    public static <T extends Entity> List<T> select(Level level, Class<T> type, AABB bounds, Predicate<T> predicate) {
        return level.getEntitiesOfClass(type, bounds, predicate);
    }
    public static <T extends Entity> Optional<T> nearest(List<T> entities, Vec3 point) {
        return entities.stream().min(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(point)));
    }
    public static void attract(Entity entity, Vec3 target, double strength, double maximumSpeed) {
        if (!Double.isFinite(strength) || strength < 0 || !Double.isFinite(maximumSpeed) || maximumSpeed <= 0) throw new IllegalArgumentException("Invalid attraction");
        if (entity.level().isClientSide) return;
        var velocity = entity.getDeltaMovement().add(target.subtract(entity.position()).normalize().scale(strength));
        if (velocity.lengthSqr() > maximumSpeed * maximumSpeed) velocity = velocity.normalize().scale(maximumSpeed);
        entity.setDeltaMovement(velocity); entity.hurtMarked = true;
    }
}
