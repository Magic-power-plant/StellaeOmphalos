package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Progress-gated node: below its level threshold the node is completely hidden (not even a
 * tooltip; visibility is evaluated on demand, never cached). Used for KEY/MAJOR/NORMAL nodes
 * whose definition carries a minimum level.
 */
public final class GatedBoonNode extends BoonNode {

    private final int minLevel;

    public GatedBoonNode(ResourceLocation id, BoonNodeType type, int gridX, int gridZ, int minLevel,
                         List<BoonModifier> modifiers, List<BoonTranslator> translators,
                         Set<ResourceLocation> requires, CompoundTag extraData) {
        super(id, type, gridX, gridZ, modifiers, translators,
                BoonUnlockRules.gated(minLevel, BoonUnlockRules.standard(requires)), extraData);
        if (type != BoonNodeType.KEY && type != BoonNodeType.MAJOR && type != BoonNodeType.NORMAL)
            throw new IllegalArgumentException("Gating is only supported for key/major/normal nodes: " + id);
        this.minLevel = minLevel;
    }

    public int minLevel() {
        return minLevel;
    }
}
