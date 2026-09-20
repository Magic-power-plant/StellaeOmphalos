package com.mpp.stellaeomphalos.player.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import com.mpp.stellaeomphalos.constellation.attribute.BoonValueBridge;
import com.mpp.stellaeomphalos.constellation.attribute.GemAffixModifier;
import com.mpp.stellaeomphalos.constellation.boon.BoonTree;
import com.mpp.stellaeomphalos.constellation.boon.SocketBoonNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Bridge provider: the player's modifiers are the union of every unlocked, unsealed node's
 * modifiers, plus the gem affixes carried by socketed gems (absolute modifiers, skipping the
 * translator chain as per contract). Translators are the concatenation of node translators.
 * Registered once during assembly; cache invalidation is driven by progress changes.
 */
public final class BoonBridgeProvider implements BoonValueBridge.Provider {

    @Override
    public List<BoonModifier> modifiersOf(Player player) {
        if (!BoonTree.ready()) return List.of();
        BoonProgress progress;
        try {
            progress = BoonProgress.get(player);
        } catch (IllegalStateException noClientView) {
            return List.of();
        }
        var tree = BoonTree.get();
        var out = new ArrayList<BoonModifier>();
        for (var id : progress.appliedNodes()) {
            if (progress.isSealed(id)) continue;
            var node = tree.node(id);
            if (node == null) continue;
            out.addAll(node.modifiers());
            if (node instanceof SocketBoonNode socket) {
                var gem = socket.contained(progress.nodeData(id));
                var affixes = readAffixes(gem);
                for (int i = 0; i < affixes.size(); i++) out.add(affixes.get(i).bind(id, node.modifiers().size() + i));
            }
        }
        return out;
    }

    @Override
    public List<BoonTranslator> translatorsOf(Player player) {
        if (!BoonTree.ready()) return List.of();
        BoonProgress progress;
        try {
            progress = BoonProgress.get(player);
        } catch (IllegalStateException noClientView) {
            return List.of();
        }
        var tree = BoonTree.get();
        var out = new ArrayList<BoonTranslator>();
        for (var id : progress.appliedNodes()) {
            if (progress.isSealed(id)) continue;
            var node = tree.node(id);
            if (node != null) out.addAll(node.translators());
        }
        return out;
    }

    /** Reads gem affixes from the item's {@code Affixes} list (the affix carrier NBT convention). */
    public static List<GemAffixModifier> readAffixes(ItemStack stack) {
        if (stack.isEmpty() || stack.getTag() == null || !stack.getTag().contains("Affixes", Tag.TAG_LIST))
            return List.of();
        var out = new ArrayList<GemAffixModifier>();
        for (var entry : stack.getTag().getList("Affixes", Tag.TAG_COMPOUND))
            out.add(GemAffixModifier.load((net.minecraft.nbt.CompoundTag) entry));
        return out;
    }
}
