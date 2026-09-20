package com.mpp.stellaeomphalos.crafting.altar.menu;

/** Shared coordinates keep all five tiers clear of action buttons and the player inventory. */
public final class AsterismMenuLayout {
    public static final int WIDTH = 230,
            HEIGHT = 248,
            BUTTON_Y = 134,
            PLAYER_LABEL_Y = 154,
            PLAYER_Y = 166,
            HOTBAR_Y = 224;

    private AsterismMenuLayout() {}

    public static int slotX(int index) {
        check(index);
        return index < 9 ? 70 + (index % 3) * 18 : 16 + ((index - 9) % 8) * 24;
    }

    public static int slotY(int index) {
        check(index);
        return index < 9 ? 28 + (index / 3) * 18 : 94 + ((index - 9) / 8) * 18;
    }

    private static void check(int index) {
        if (index < 0 || index >= 25) throw new IndexOutOfBoundsException(index);
    }
}
