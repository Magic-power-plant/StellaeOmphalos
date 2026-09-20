package com.mpp.stellaeomphalos.client.codex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Page drawing primitives keep graphics API use out of common page definitions. */
public record CodexDrawContext(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
    public void item(ItemStack stack, int px, int py) {
        graphics.renderItem(stack, x + px, y + py);
    }

    public void text(Component text, int px, int py, int color) {
        graphics.drawString(Minecraft.getInstance().font, text, x + px, y + py, color, false);
    }

    public void rectangle(int px, int py, int width, int height, int color) {
        graphics.fill(x + px, y + py, x + px + width, y + py + height, color);
    }
}
