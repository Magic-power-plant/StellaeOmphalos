package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class BoonModifier {

    public enum Mode {
        ADDITION,
        ADDED_MULTIPLY,
        STACKING_MULTIPLY;

        public AttributeModifier.Operation toVanilla() {
            return AttributeModifier.Operation.fromValue(ordinal());
        }

        public static Mode fromOrdinal(int ordinal) {
            var values = values();
            if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("bad mode ordinal " + ordinal);
            return values[ordinal];
        }
    }

    private final BoonAttribute attribute;
    private final Mode mode;
    private final double value;
    private final boolean absolute;
    @Nullable
    private ResourceLocation ownerNode;
    private int index = -1;

    public BoonModifier(BoonAttribute attribute, Mode mode, double value, boolean absolute) {
        if (attribute == null || mode == null) throw new IllegalArgumentException("null attribute/mode");
        if (!Double.isFinite(value)) throw new IllegalArgumentException("non-finite value for " + attribute.id());
        if (mode == Mode.ADDITION && attribute.isOnlyMultiplicative())
            throw new IllegalArgumentException(attribute.id() + " is only multiplicative");
        this.attribute = attribute;
        this.mode = mode;
        this.value = value;
        this.absolute = absolute;
    }

    public BoonAttribute attribute() {
        return attribute;
    }

    public Mode mode() {
        return mode;
    }

    public double value() {
        return value;
    }

    public boolean absolute() {
        return absolute;
    }

    public BoonModifier bind(ResourceLocation ownerNode, int index) {
        if (ownerNode == null || index < 0) throw new IllegalArgumentException("bad identity " + ownerNode + "#" + index);
        this.ownerNode = ownerNode;
        this.index = index;
        return this;
    }

    @Nullable
    public ResourceLocation ownerNode() {
        return ownerNode;
    }

    public int index() {
        return index;
    }

    public String identity() {
        return ownerNode == null ? "unbound:" + index : ownerNode + "#" + index;
    }

    public BoonModifier withValue(double newValue) {
        var copy = new BoonModifier(attribute, mode, newValue, absolute);
        copy.ownerNode = ownerNode;
        copy.index = index;
        return copy;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s[%s %+g%s]", identity(), mode, value, absolute ? " absolute" : "");
    }
}
