package com.mpp.stellaeomphalos.crafting.altar.recipe;

import java.util.Locale;

public enum AsterismTier implements net.minecraft.util.StringRepresentable {
    DISCOVERY(9, 1000, false),
    RESONANCE(13, 2000, true),
    SIGN(21, 4000, true),
    TRAIT(25, 8000, true),
    RADIANCE(25, 16000, false);
    private final int slots;
    private final long capacity;
    private final boolean structure;

    AsterismTier(int slots, long capacity, boolean structure) {
        this.slots = slots;
        this.capacity = capacity;
        this.structure = structure;
    }

    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int menuCode() {
        return switch (this) {
            case DISCOVERY -> 0;
            case RESONANCE -> 1;
            case SIGN -> 2;
            case TRAIT -> 3;
            case RADIANCE -> 4;
        };
    }

    public int accessibleSlotCount() {
        return 25;
    }

    public int visibleSlotCount() {
        return slots;
    }

    public long lumenCapacity() {
        return capacity;
    }

    public boolean requiresStructure() {
        return structure;
    }

    public AsterismTier downgradeFloor() {
        return DISCOVERY;
    }

    public int maxConcurrentCrafts() {
        return 1;
    }

    public boolean supports(AsterismTier required) {
        return compareTo(required) >= 0;
    }

    public static AsterismTier parse(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
