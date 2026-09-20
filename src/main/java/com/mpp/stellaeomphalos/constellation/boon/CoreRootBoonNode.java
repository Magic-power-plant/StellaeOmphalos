package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Core root node: requires every major sign discovered, consumes no skill point, and grants
 * three free skill points through fixed token names. Because the tokens are fixed set entries,
 * re-granting them is idempotent (the player receives the bonus exactly once per unlock).
 */
public final class CoreRootBoonNode extends BoonNode {

    public static final List<String> BONUS_TOKENS = List.of(
            "stellaeomphalos:core/root#1", "stellaeomphalos:core/root#2", "stellaeomphalos:core/root#3");

    public CoreRootBoonNode(ResourceLocation id, int gridX, int gridZ,
                            List<BoonModifier> modifiers, List<BoonTranslator> translators, CompoundTag extraData) {
        super(id, BoonNodeType.CORE_ROOT, gridX, gridZ, modifiers, translators, BoonUnlockRules.coreRoot(), extraData);
    }
}
