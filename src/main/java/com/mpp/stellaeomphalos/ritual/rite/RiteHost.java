package com.mpp.stellaeomphalos.ritual.rite;

import com.mpp.stellaeomphalos.structure.match.StructureState;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Capabilities required by the lifecycle. commitOutputs is atomic or returns false unchanged. */
public interface RiteHost {
    StructureState structureState();

    boolean crystalValid(RiteRecipe recipe);

    boolean celestialReady(RiteRecipe recipe);

    boolean amplifiersReady(RiteRecipe recipe);

    long storedLumen();

    void consumeLumen(int amount);

    int amplifierUpkeep();

    boolean commitOutputs(List<ItemStack> outputs);

    void cycleCompleted();

    void stateChanged(RiteState previous, RiteState current);
}
