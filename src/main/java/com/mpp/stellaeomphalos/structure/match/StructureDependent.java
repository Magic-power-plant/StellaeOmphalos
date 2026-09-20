package com.mpp.stellaeomphalos.structure.match;

public interface StructureDependent {
    void onStructureStateChanged(StructureState state, StructureState previous);

    StructureState structureState();
}
