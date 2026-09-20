package com.mpp.stellaeomphalos.client.codex;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface CodexPageView {
    void layout(int width, int height);

    void draw(CodexDrawContext context, long tick);

    Optional<ItemStack> hitTest(double x, double y);

    boolean drag(double deltaX);

    boolean scroll(double delta);
}
