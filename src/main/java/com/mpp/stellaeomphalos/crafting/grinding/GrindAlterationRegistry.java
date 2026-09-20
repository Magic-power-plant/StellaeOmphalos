package com.mpp.stellaeomphalos.crafting.grinding;

import com.mpp.stellaeomphalos.constellation.attribute.CrystalAttunement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;

import java.util.*;

public final class GrindAlterationRegistry {
    private static final Map<ResourceLocation, GrindAlteration> TYPES = new LinkedHashMap<>();

    static {
        register("crystal", new Traits("CrystalTraits"));
        register("tool", new Traits("ToolTraits"));
        register(
                "hone",
                new GrindAlteration() {
                    public boolean matches(ItemStack s) {
                        return s.getItem() instanceof SwordItem
                                && (!s.hasTag() || !s.getTag().getBoolean("Honed"));
                    }

                    public GrindOutcome apply(ItemStack s, RandomSource r) {
                        var output = s.copy();
                        output.getOrCreateTag().putBoolean("Honed", true);
                        return new GrindOutcome(GrindOutcome.Kind.ITEM_CHANGE, output);
                    }
                });
    }

    private GrindAlterationRegistry() {}

    public static void register(String id, GrindAlteration type) {
        if (TYPES.putIfAbsent(new ResourceLocation("stellaeomphalos", id), type) != null)
            throw new IllegalArgumentException("Duplicate grind alteration");
    }

    public static GrindAlteration get(ResourceLocation id) {
        var value = TYPES.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown grind alteration " + id);
        return value;
    }

    private record Traits(String key) implements GrindAlteration {
        public boolean matches(ItemStack s) {
            return s.hasTag() && s.getTag().contains(key, 10);
        }

        public GrindOutcome apply(ItemStack s, RandomSource random) {
            var copy = s.copy();
            var old = CrystalAttunement.load(copy.getTag().getCompound(key));
            var changed = old.grind(random, copy.getTag().getInt("GrindFailures"));
            if (changed == null)
                return new GrindOutcome(GrindOutcome.Kind.FAIL_BREAK_ITEM, ItemStack.EMPTY);
            copy.getTag().put(key, changed.save());
            copy.getTag()
                    .putInt(
                            "GrindFailures",
                            changed.fracture() > old.fracture()
                                    ? Math.min(32, copy.getTag().getInt("GrindFailures") + 1)
                                    : 0);
            return new GrindOutcome(GrindOutcome.Kind.ITEM_CHANGE, copy);
        }
    }
}
