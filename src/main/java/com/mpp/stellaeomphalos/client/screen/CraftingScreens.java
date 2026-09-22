package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.crafting.altar.menu.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Functional menus for Part 3; authored skins and effects are supplied by Part 7. */
@Mod.EventBusSubscriber(
        modid = Omphalos.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class CraftingScreens {
    private CraftingScreens() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> {
                    for (String kind :
                            java.util.List.of(
                                    "asterism", "lumen_infuser", "quern", "lumen_well"))
                        MenuScreens.register(MachineMenus.type(kind), MachineScreen::new);
                    MenuScreens
                            .<net.minecraft.world.inventory.CraftingMenu, CraftingScreen>register(
                                    MachineMenus.WORKBENCH.get(), CraftingScreen::new);
                });
    }

    public static final class MachineScreen extends AbstractContainerScreen<AsterismMenu> {
        public MachineScreen(AsterismMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            imageWidth = AsterismMenuLayout.WIDTH;
            imageHeight = AsterismMenuLayout.HEIGHT;
            inventoryLabelY = AsterismMenuLayout.PLAYER_LABEL_Y;
            inventoryLabelX = 34;
        }

        @Override
        protected void init() {
            super.init();
            for (int i = 0; i < 3; i++) {
                final int action = i;
                String key =
                        switch (i) {
                            case 0 -> "start";
                            case 1 -> "abort";
                            default -> "collect";
                        };
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable("stellaeomphalos.crafting." + key),
                                        button -> {
                                            if (minecraft != null && minecraft.gameMode != null)
                                                minecraft.gameMode.handleInventoryButtonClick(
                                                        menu.containerId, action);
                                        })
                                .bounds(
                                        leftPos + 12 + i * 72,
                                        topPos + AsterismMenuLayout.BUTTON_Y,
                                        66,
                                        16)
                                .build());
            }
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF192636);
            for (var slot : menu.slots) {
                graphics.fill(
                        leftPos + slot.x - 1,
                        topPos + slot.y - 1,
                        leftPos + slot.x + 17,
                        topPos + slot.y + 17,
                        0xFF8197A8);
                graphics.fill(
                        leftPos + slot.x,
                        topPos + slot.y,
                        leftPos + slot.x + 16,
                        topPos + slot.y + 16,
                        0xFF0E1824);
            }
            graphics.fill(leftPos + 12, topPos + 16, leftPos + 218, topPos + 21, 0xFF0C121B);
            graphics.fill(
                    leftPos + 12,
                    topPos + 16,
                    leftPos + 12 + 206 * menu.data().get(3) / 1000,
                    topPos + 21,
                    0xFF9CDFF1);
        }

        @Override
        public void render(GuiGraphics graphics, int x, int y, float partial) {
            renderBackground(graphics);
            super.render(graphics, x, y, partial);
            renderTooltip(graphics, x, y);
        }

        @Override
        protected void renderLabels(GuiGraphics graphics, int x, int y) {
            if (menu.data().get(6) > 0) {
                graphics.drawString(
                        font,
                        Component.translatable("stellaeomphalos.crafting.ready"),
                        156,
                        62,
                        0xBBDDEE,
                        false);
                if (minecraft != null
                        && minecraft.level != null
                        && minecraft.level.getBlockEntity(menu.position())
                                instanceof
                                com.mpp.stellaeomphalos.content.blockentity.crafting
                                                        .AbstractCraftingMachine
                                                machine) {
                    var output = machine.pendingOutput();
                    graphics.renderItem(output, 184, 74);
                    graphics.renderItemDecorations(font, output, 184, 74);
                }
            }

            graphics.drawString(font, title, 8, 5, 0xE3F2FF, false);
            graphics.drawString(
                    font,
                    Component.translatable(
                            "stellaeomphalos.crafting.energy",
                            (menu.data().get(0) & 65535) | menu.data().get(1) << 16,
                            menu.data().get(2)),
                    12,
                    82,
                    0xBBDDEE,
                    false);
        }
    }
}
