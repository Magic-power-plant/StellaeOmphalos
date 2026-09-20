package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

/**
 * Socket node: holds one gem per player. The gem lives in the player's per-node private data
 * under {@link #SOCKET_KEY}; writes snapshot the tag first and run through the progress layer's
 * {@code mutate} callback so sync/validation stay in one place. Removing the gem returns it to
 * the player — when the inventory is full it drops to the ground and is never voided (AC-2.17).
 */
public final class SocketBoonNode extends BoonNode {

    public static final String SOCKET_KEY = "SocketedItem";
    private static volatile TagKey<Item> gemTag;

    /** The boon_gem item tag, resolved lazily so pure-logic tests never touch vanilla registries. */
    public static TagKey<Item> gemTag() {
        var tag = gemTag;
        if (tag == null) {
            tag = TagKey.create(Registries.ITEM, new ResourceLocation(Omphalos.MODID, "boon_gem"));
            gemTag = tag;
        }
        return tag;
    }

    public SocketBoonNode(ResourceLocation id, int gridX, int gridZ,
                          List<BoonModifier> modifiers, List<BoonTranslator> translators,
                          Set<ResourceLocation> requires, CompoundTag extraData) {
        super(id, BoonNodeType.SOCKET, gridX, gridZ, modifiers, translators, BoonUnlockRules.standard(requires), extraData);
    }

    public ItemStack contained(CompoundTag nodeData) {
        if (!nodeData.contains(SOCKET_KEY, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        return ItemStack.of(nodeData.getCompound(SOCKET_KEY));
    }

    /** A gem is a stack in the boon_gem item tag or one already carrying affix NBT. */
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(gemTag()) || (stack.getTag() != null && stack.getTag().contains("Affixes", Tag.TAG_LIST));
    }

    /**
     * Writes the socketed stack (empty stack clears the slot). Refused while the node is not
     * unlocked for the player or the stack is not an acceptable gem. The node data is snapshotted
     * before the change and restored if the progress layer's callback throws.
     */
    public boolean setContained(ServerPlayer player, BoonProgressView progress, CompoundTag nodeData,
                                ItemStack stack, @Nullable Runnable mutate) {
        if (!progress.hasNode(id())) return false;
        if (!stack.isEmpty() && !accepts(stack)) return false;
        boolean hadPrevious = nodeData.contains(SOCKET_KEY, Tag.TAG_COMPOUND);
        var previous = hadPrevious ? nodeData.getCompound(SOCKET_KEY).copy() : null;
        try {
            if (stack.isEmpty()) nodeData.remove(SOCKET_KEY);
            else {
                var single = stack.copy();
                single.setCount(1);
                nodeData.put(SOCKET_KEY, single.save(new CompoundTag()));
            }
            if (mutate != null) mutate.run();
        } catch (RuntimeException exception) {
            nodeData.remove(SOCKET_KEY);
            if (previous != null) nodeData.put(SOCKET_KEY, previous);
            throw exception;
        }
        return true;
    }

    /** Server-side only: returns the gem to the player, dropping it at their feet when full. */
    public void dropToPlayer(ServerPlayer player, CompoundTag nodeData) {
        var stack = contained(nodeData);
        if (stack.isEmpty()) return;
        nodeData.remove(SOCKET_KEY);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
