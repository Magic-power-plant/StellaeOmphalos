package com.mpp.stellaeomphalos.constellation.effect;

import com.mpp.stellaeomphalos.constellation.domain.DomainContext;
import com.mpp.stellaeomphalos.constellation.domain.DomainEntityScan;
import com.mpp.stellaeomphalos.constellation.domain.DomainParticles;
import com.mpp.stellaeomphalos.constellation.domain.DomainProperties;
import com.mpp.stellaeomphalos.constellation.sign.MajorSign;
import com.mpp.stellaeomphalos.core.util.combat.DamageKit;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Animal;

import javax.annotation.Nullable;

/**
 * bootes — herding. Two-stage sampling ({@code herdChance} then {@code dropChance}); grazable
 * animals shed produce, at most 8 drops per tick; scan cap 16. Corrupted: slays animals with high
 * starlight damage via {@link DamageKit}.
 */
public final class DomainEffectHerd extends DomainEntityScan<Animal> {
    private static final int MAX_TARGETS = 16;
    private static final int MAX_DROPS_PER_TICK = 8;
    private static final float HERD_CHANCE = 0.20F;
    private static final float DROP_CHANCE = 0.35F;
    private static final float CORRUPTED_DAMAGE = 500.0F;

    public DomainEffectHerd(@Nullable MajorSign owner) {
        super(owner, Animal.class, MAX_TARGETS);
        setSearchFilter(
                animal ->
                        com.mpp.stellaeomphalos.core.platform.WorldBehaviorBridge.tables()
                                .herdable(animal));
    }

    @Override
    public DomainProperties provideProperties(int mirrorCount) {
        return new DomainProperties(6.0, 1.0, 1.0, false, 0.0, 1.0);
    }

    @Override
    public boolean play(DomainContext ctx, float strength, DomainProperties props) {
        var level = ctx.level();
        if (!strengthGate(strength, level.random)) return false;
        if (props.corrupted()) return playCorrupted(ctx, props);
        if (level.random.nextFloat() >= HERD_CHANCE) return false;
        int drops = 0;
        for (var animal : collect(level, ctx.origin(), props.size())) {
            if (drops >= MAX_DROPS_PER_TICK) break;
            if (animal.isBaby() || level.random.nextFloat() >= DROP_CHANCE * props.potency())
                continue;
            var produce =
                    com.mpp.stellaeomphalos.core.platform.WorldBehaviorBridge.tables()
                            .herdDrops(level, animal);
            for (var stack : produce) {
                if (drops >= MAX_DROPS_PER_TICK) break;
                animal.spawnAtLocation(stack);
                drops++;
            }
            DomainParticles.broadcast(
                    level,
                    animal.blockPosition(),
                    96.0,
                    PktDomainParticle.Types.HERD,
                    ctx.origin(),
                    level.random.nextLong());
        }
        return drops > 0;
    }

    private boolean playCorrupted(DomainContext ctx, DomainProperties props) {
        var level = ctx.level();
        var holder =
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolder(DomainDamageTypes.STARLIGHT)
                        .orElse(
                                level.registryAccess()
                                        .registryOrThrow(Registries.DAMAGE_TYPE)
                                        .getHolderOrThrow(
                                                net.minecraft.world.damagesource.DamageTypes
                                                        .MAGIC));
        var source = new DamageSource(holder);
        boolean acted = false;
        for (var animal : collect(level, ctx.origin(), props.size()))
            acted |=
                    DamageKit.apply(
                            animal,
                            source,
                            (float) (CORRUPTED_DAMAGE * Math.max(props.potency(), 1.0)));
        return acted;
    }
}
