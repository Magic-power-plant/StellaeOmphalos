package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Key node: a pivotal ability node; rules match the standard rule, only presentation differs. */
public final class KeyBoonNode extends BoonNode {

    public KeyBoonNode(ResourceLocation id, int gridX, int gridZ,
                       List<BoonModifier> modifiers, List<BoonTranslator> translators,
                       Set<ResourceLocation> requires, CompoundTag extraData) {
        super(id, BoonNodeType.KEY, gridX, gridZ, modifiers, translators, BoonUnlockRules.standard(requires), extraData);
    }
}
