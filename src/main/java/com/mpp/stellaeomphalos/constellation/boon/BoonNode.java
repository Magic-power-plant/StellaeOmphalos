package com.mpp.stellaeomphalos.constellation.boon;

import com.mpp.stellaeomphalos.constellation.attribute.BoonModifier;
import com.mpp.stellaeomphalos.constellation.attribute.BoonTranslator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * One node of the boon tree. Instances are built by the data-table loader, wired through
 * {@link BoonGraph} and sealed when the owning {@link BoonTree} freezes. Per-player node data
 * (gem sockets, behavior history) lives in the player progress store; {@link #extraData()} here
 * is definition-level private data attached by the loader.
 */
public class BoonNode {

    private final ResourceLocation id;
    private final BoonNodeType type;
    private final int gridX;
    private final int gridZ;
    private final List<BoonModifier> modifiers;
    private final List<BoonTranslator> translators;
    private final UnlockRule rule;
    private final CompoundTag extraData;
    private final Set<ResourceLocation> links = new LinkedHashSet<>();
    private boolean frozen;

    public BoonNode(ResourceLocation id, BoonNodeType type, int gridX, int gridZ,
                    List<BoonModifier> modifiers, List<BoonTranslator> translators, UnlockRule rule, CompoundTag extraData) {
        if (id == null || type == null || rule == null) throw new IllegalArgumentException("null node component for " + id);
        this.id = id;
        this.type = type;
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.modifiers = List.copyOf(modifiers);
        this.translators = List.copyOf(translators);
        this.rule = rule;
        this.extraData = extraData == null ? new CompoundTag() : extraData.copy();
        for (int i = 0; i < this.modifiers.size(); i++) this.modifiers.get(i).bind(id, i);
    }

    public ResourceLocation id() {
        return id;
    }

    public BoonNodeType type() {
        return type;
    }

    public int gridX() {
        return gridX;
    }

    public int gridZ() {
        return gridZ;
    }

    public List<BoonModifier> modifiers() {
        return modifiers;
    }

    public List<BoonTranslator> translators() {
        return translators;
    }

    public UnlockRule rule() {
        return rule;
    }

    /** Definition-level private data; treated as read-only once the tree is frozen. */
    public CompoundTag extraData() {
        return extraData;
    }

    /** Symmetric adjacency as an id view. */
    public Set<ResourceLocation> neighbors() {
        return Set.copyOf(links);
    }

    void link(BoonNode other) {
        if (frozen) throw new IllegalStateException("Boon tree is frozen");
        links.add(other.id());
    }

    void unlink(BoonNode other) {
        if (frozen) throw new IllegalStateException("Boon tree is frozen");
        links.remove(other.id());
    }

    void freeze() {
        frozen = true;
    }

    boolean frozen() {
        return frozen;
    }

    /** Node states for one progress view, evaluated on demand; hidden gated nodes yield empty. */
    public java.util.Optional<BoonNodeState> stateFor(net.minecraft.server.level.ServerPlayer player, BoonProgressView progress) {
        if (progress.isSealed(id)) return java.util.Optional.of(BoonNodeState.SEALED);
        if (!rule.visible(player, progress)) return java.util.Optional.empty();
        if (progress.hasNode(id)) return java.util.Optional.of(BoonNodeState.ALLOCATED);
        return java.util.Optional.of(rule.mayUnlock(player, this, progress) ? BoonNodeState.UNLOCKABLE : BoonNodeState.UNALLOCATED);
    }

    List<ResourceLocation> linkView() {
        return new ArrayList<>(links);
    }
}
