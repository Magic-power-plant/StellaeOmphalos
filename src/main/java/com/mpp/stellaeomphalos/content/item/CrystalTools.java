package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.constellation.attribute.ToolCrystalAttunement;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;

/** Native tool actions and harvest tiers, with crystal quality supplied by the tool capability. */
public final class CrystalTools {
    private CrystalTools() {}

    public interface Attuned {
        default float attunedSpeed(ItemStack stack, float vanillaSpeed) {
            if (vanillaSpeed <= 1 || !stack.hasTag() || !stack.getTag().contains("ToolTraits")) return vanillaSpeed;
            var traits = stack.getTag().getCompound("ToolTraits");
            double collect = Math.max(0, Math.min(traits.getInt("Collect"),
                    traits.contains("MaxCollect") ? Math.max(1, traits.getInt("MaxCollect")) : 100));
            var quality = new ToolCrystalAttunement(traits.getInt("Size"), traits.getInt("Purity"),
                    collect, traits.getInt("Fracture"));
            return (float) (vanillaSpeed * quality.efficiency());
        }
    }

    public static final class Pickaxe extends PickaxeItem implements Attuned {
        public Pickaxe() { super(Tiers.DIAMOND, 1, -2.8F, new Item.Properties()); }
        @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
            return attunedSpeed(stack, super.getDestroySpeed(stack, state));
        }
    }
    public static final class Axe extends AxeItem implements Attuned {
        public Axe() { super(Tiers.DIAMOND, 5, -3, new Item.Properties()); }
        @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
            return attunedSpeed(stack, super.getDestroySpeed(stack, state));
        }
    }
    public static final class Shovel extends ShovelItem implements Attuned {
        public Shovel() { super(Tiers.DIAMOND, 1.5F, -3, new Item.Properties()); }
        @Override public float getDestroySpeed(ItemStack stack, BlockState state) {
            return attunedSpeed(stack, super.getDestroySpeed(stack, state));
        }
    }
}
