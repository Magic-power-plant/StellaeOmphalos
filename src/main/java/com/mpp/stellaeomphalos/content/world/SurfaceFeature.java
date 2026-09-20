package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.OmphalosConfig;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

public final class SurfaceFeature extends Feature<SimpleBlockConfiguration> {
    private final boolean sand;

    public SurfaceFeature(boolean sand) {
        super(SimpleBlockConfiguration.CODEC);
        this.sand = sand;
    }

    public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
        if (!OmphalosConfig.COMMON.flag("worldgen.enabled")) return false;
        var p = ctx.origin();
        var level = ctx.level();
        if (!level.ensureCanWrite(p)) return false;
        var result = ctx.config().toPlace().getState(ctx.random(), p);
        if (sand) {
            if (!level.getBlockState(p).is(BlockTags.SAND)) return false;
            return level.setBlock(p, result, 2);
        }
        if (!level.getBlockState(p).canBeReplaced() || !result.canSurvive(level, p)) return false;
        boolean placed = level.setBlock(p, result, 2);
        if (placed && ctx.random().nextInt(4) == 0)
            for (int n = 0; n < 1 + ctx.random().nextInt(4); n++) {
                var q = p.offset(ctx.random().nextInt(15) - 7, 0, ctx.random().nextInt(15) - 7);
                if (level.ensureCanWrite(q)
                        && level.getBlockState(q).canBeReplaced()
                        && result.canSurvive(level, q)) level.setBlock(q, result, 2);
            }
        return placed;
    }
}
