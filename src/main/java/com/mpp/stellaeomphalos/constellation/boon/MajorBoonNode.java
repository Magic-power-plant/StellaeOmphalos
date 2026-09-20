package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Major node: large attribute bonus, standard unlock rule (UI scales it up on the client). */
public final class MajorBoonNode extends BoonNode {

    public MajorBoonNode(ResourceLocation id, int gridX, int gridZ,
                         List<BoonModifier> modifiers, List<BoonTranslator> translators,
                         Set<ResourceLocation> requires, CompoundTag extraData) {
        super(id, BoonNodeType.MAJOR, gridX, gridZ, modifiers, translators, BoonUnlockRules.standard(requires), extraData);
    }
}
