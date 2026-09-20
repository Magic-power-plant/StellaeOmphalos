package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Sign root node: unlockable once the sign is discovered and the core root is unlocked,
 * consumes no skill point. Carries the experience multiplier applied by the sign's
 * behavior-to-experience hook (player module).
 */
public final class RootBoonNode extends BoonNode {

    private final ResourceLocation signId;
    private final double expMultiplier;

    public RootBoonNode(ResourceLocation id, int gridX, int gridZ, ResourceLocation signId, double expMultiplier,
                        List<BoonModifier> modifiers, List<BoonTranslator> translators, CompoundTag extraData) {
        super(id, BoonNodeType.ROOT, gridX, gridZ, modifiers, translators, BoonUnlockRules.root(signId), extraData);
        if (expMultiplier <= 0 || !Double.isFinite(expMultiplier)) throw new IllegalArgumentException("bad exp multiplier");
        this.signId = signId;
        this.expMultiplier = expMultiplier;
    }

    public ResourceLocation signId() {
        return signId;
    }

    /** Multiplies experience granted by the root's behavior hook. */
    public double expMultiplier() {
        return expMultiplier;
    }
}
