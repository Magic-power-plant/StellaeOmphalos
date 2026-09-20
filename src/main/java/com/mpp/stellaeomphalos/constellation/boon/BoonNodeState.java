package com.mpp.stellaeomphalos.constellation.boon;

/**
 * Render state of a node for one player. SEALED nodes stay on the tree but cannot be
 * unlocked and do not consume skill points.
 */
public enum BoonNodeState {
    UNALLOCATED,
    UNLOCKABLE,
    ALLOCATED,
    SEALED
}
