package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.OmphalosConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigOverviewScreen extends Screen {
    private final Screen parent;
    private OmphalosConfig.Section section = OmphalosConfig.CLIENT;
    private final List<Map.Entry<String, Object>> rows = new ArrayList<>();
    private boolean restartRequired;
    public boolean restartRequired() { return restartRequired; }
    private int page;
    private int pageSize;
    public ConfigOverviewScreen(Screen parent) { super(Component.translatable("stellaeomphalos.config.title")); this.parent = parent; restartRequired = parent instanceof ConfigOverviewScreen config && config.restartRequired(); }
    @Override protected void init() {
        pageSize = Math.max(1, (height - 116) / 26);
        rows.clear();
        var values = new java.util.LinkedHashMap<>(section.snapshot());
        if (section == OmphalosConfig.SERVER)
            com.mpp.stellaeomphalos.client.OmphalosClient.mirrors()
                    .snapshot(com.mpp.stellaeomphalos.network.sync.ServerConfigDataset.ID).ifPresent(data -> {
                        for (String key : data.getAllKeys()) values.put(key, data.getString(key));
                    });
        values.entrySet().stream().filter(entry -> OmphalosConfig.supported(entry.getKey())).forEach(rows::add);
        rows.sort(Comparator.comparing(Map.Entry::getKey));
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.translatable("stellaeomphalos.config.client"), button -> change(OmphalosConfig.CLIENT)).bounds(center - 154, 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("stellaeomphalos.config.common"), button -> change(OmphalosConfig.COMMON)).bounds(center - 50, 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("stellaeomphalos.config.server"), button -> change(OmphalosConfig.SERVER)).bounds(center + 54, 30, 100, 20).build());
        for (int i = page * pageSize; i < Math.min(rows.size(), (page + 1) * pageSize); i++) {
            var row = rows.get(i); int y = 60 + (i - page * pageSize) * 26;
            if (row.getValue() instanceof Boolean flag) {
                var toggle = addRenderableWidget(new net.minecraft.client.gui.components.Checkbox(center + 55, y, 100, 20,
                        Component.translatable(flag ? "options.on" : "options.off"), flag) {
                    @Override public void onPress() { restartRequired |= section.needsRestart(row.getKey()); section.toggle(row.getKey()); rebuildWidgets(); }
                });
                toggle.active = section != OmphalosConfig.SERVER && section.spec().isLoaded();
            } else if (row.getValue() instanceof Enum<?> value) {
                var toggle = addRenderableWidget(Button.builder(Component.literal(value.name()), button -> {
                    var all = value.getDeclaringClass().getEnumConstants();
                    restartRequired |= section.needsRestart(row.getKey()); section.set(row.getKey(), all[(value.ordinal() + 1) % all.length].name()); rebuildWidgets();
                }).bounds(center + 55, y, 100, 20).build());
                toggle.active = section != OmphalosConfig.SERVER && section.spec().isLoaded();
            } else {
                var input = addRenderableWidget(new EditBox(font, center + 55, y, 68, 20, Component.literal(row.getKey())));
                input.setMaxLength(4096);
                input.setValue(row.getValue() instanceof List<?> list
                        ? new com.google.gson.Gson().toJson(list) : row.getValue().toString());
                boolean editable = section != OmphalosConfig.SERVER && section.spec().isLoaded();
                input.setEditable(editable);
                var apply = addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    try { restartRequired |= section.needsRestart(row.getKey()); section.set(row.getKey(), input.getValue()); input.setTextColor(0xFFFFFF); }
                    catch (RuntimeException exception) { input.setTextColor(0xFF6666); }
                }).bounds(center + 127, y, 28, 20).build());
                apply.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("stellaeomphalos.config.apply")));
                apply.active = editable;
            }
        }
        var previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; rebuildWidgets(); }).bounds(center - 154, height - 30, 30, 20).build());
        previous.active = page > 0;
        var next = addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; rebuildWidgets(); }).bounds(center + 124, height - 30, 30, 20).build());
        next.active = (page + 1) * pageSize < rows.size();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(center - 50, height - 30, 100, 20).build());
    }
    private void change(OmphalosConfig.Section section) { this.section = section; page = 0; rebuildWidgets(); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        for (int i = page * pageSize; i < Math.min(rows.size(), (page + 1) * pageSize); i++) {
            var text = Component.translatable("stellaeomphalos.config." + rows.get(i).getKey());
            graphics.drawString(font, font.substrByWidth(text, 205).getString(), width / 2 - 154, 66 + (i - page * pageSize) * 26, 0xFFFFFF);
        }
        if(restartRequired)graphics.drawCenteredString(font,Component.translatable("stellaeomphalos.config.restart_required"),width/2,height-44,0xffd696);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { if(parent instanceof ConfigOverviewScreen config)config.restartRequired |= restartRequired; minecraft.setScreen(parent); }
}
