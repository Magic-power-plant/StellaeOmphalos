package com.mpp.stellaeomphalos.crafting.altar.recipe;

public enum CraftState {
    IDLE,
    RUNNING,
    SUSPENDED,
    PAUSED,
    ORPHANED,
    FINISHED;

    public int menuCode() {
        return switch (this) {
            case IDLE -> 0;
            case RUNNING -> 1;
            case SUSPENDED -> 2;
            case PAUSED -> 3;
            case ORPHANED -> 4;
            case FINISHED -> 5;
        };
    }
}
