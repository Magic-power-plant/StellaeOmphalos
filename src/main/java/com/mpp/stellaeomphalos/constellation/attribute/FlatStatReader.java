package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.world.entity.player.Player;

/** 平值直显。 */
public class FlatStatReader extends BoonStatReader {

    private final String suffix;
    private final String note;

    public FlatStatReader(BoonAttribute attribute) {
        this(attribute, "");
    }

    public FlatStatReader(BoonAttribute attribute, String suffix) {
        this(attribute, suffix, "");
    }

    public FlatStatReader(BoonAttribute attribute, String suffix, String note) {
        super(attribute);
        this.suffix = suffix;
        this.note = note;
    }

    @Override
    public BoonStatLine display(Player player) {
        return format(BoonValueBridge.value(player, attribute));
    }

    @Override
    public BoonStatLine format(double value) {
        return new BoonStatLine(attribute, nameKey(), value, suffix, note);
    }
}
