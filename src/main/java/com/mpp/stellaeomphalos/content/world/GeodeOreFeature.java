package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.OmphalosConfig;

import net.minecraft.core.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public final class GeodeOreFeature extends Feature<OreConfiguration> {
    public GeodeOreFeature() {
        super(OreConfiguration.CODEC);
    }

    public boolean place(FeaturePlaceContext<OreConfiguration> ctx) {
        if (!OmphalosConfig.COMMON.flag("worldgen.enabled")) return false;
        boolean placed = placeOne(ctx, ctx.origin());
        if (placed && ctx.random().nextInt(4) == 0)
            placeOne(
                    ctx,
                    ctx.origin()
                            .relative(Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random())));
        return placed;
    }

    private boolean placeOne(FeaturePlaceContext<OreConfiguration> ctx, BlockPos p) {
        var level = ctx.level();
        if (!level.ensureCanWrite(p)) return false;
        var state = level.getBlockState(p);
        for (var target : ctx.config().targetStates)
            if (target.target.test(state, ctx.random())) {
                if (level.setBlock(p, target.state, 2)) {
                    PendingGeodeRegistration.enqueue(level.getLevel().dimension(), p);
                    return true;
                }
            }
        return false;
    }
}
