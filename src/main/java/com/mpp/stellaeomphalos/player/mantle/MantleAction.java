package com.mpp.stellaeomphalos.player.mantle;

public record MantleAction(Kind kind, long tick, float damage, boolean fire, boolean water) {
    public enum Kind {
        TICK,
        HURT,
        ATTACK,
        BREAK,
        KILL
    }
}
