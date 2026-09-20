package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Connector node: unlockable when every neighbor is already unlocked or any connector is
 * unlocked somewhere on the tree. On unlock the progress layer force-opens every adjacent node
 * and grants one free-point token per opened neighbor; the token name is derived from both node
 * ids so the grant can be reclaimed precisely when the opening is rolled back.
 */
public final class ConnectorBoonNode extends BoonNode {

    public ConnectorBoonNode(ResourceLocation id, int gridX, int gridZ,
                             List<BoonModifier> modifiers, List<BoonTranslator> translators, CompoundTag extraData) {
        super(id, BoonNodeType.CONNECTOR, gridX, gridZ, modifiers, translators, BoonUnlockRules.connector(), extraData);
    }

    /** Fixed token name for the free point granted by force-opening {@code neighborId}. */
    public String tokenFor(ResourceLocation neighborId) {
        return "connector:" + id() + "->" + neighborId;
    }
}
