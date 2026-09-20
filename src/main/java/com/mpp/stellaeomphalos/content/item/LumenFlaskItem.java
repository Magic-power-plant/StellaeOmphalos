package com.mpp.stellaeomphalos.content.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import java.util.List;

/** Reusable two-bucket container makes precise fluid recipe inputs available without other mods. */
public final class LumenFlaskItem extends Item {
    public LumenFlaskItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
        return new FluidHandlerItemStack(stack, 2000);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        FluidUtil.getFluidContained(stack)
                .ifPresent(
                        fluid ->
                                tooltip.add(
                                        Component.translatable(
                                                "stellaeomphalos.crafting.flask_contents",
                                                fluid.getDisplayName(),
                                                fluid.getAmount())));
    }
}
