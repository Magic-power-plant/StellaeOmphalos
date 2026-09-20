package com.mpp.stellaeomphalos.client.codex;

import com.mpp.stellaeomphalos.client.boon.BoonMirror;
import com.mpp.stellaeomphalos.core.platform.RecipeDisplayData;
import com.mpp.stellaeomphalos.crafting.altar.menu.AsterismMenuLayout;
import com.mpp.stellaeomphalos.knowledge.codex.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

import java.util.*;

/** Recreated on page changes. Text layout and hit boxes are never shared between views. */
public final class StandardCodexPageView implements CodexPageView {
    private record ItemFrame(int x, int y, ItemStack stack) {}

    private final CodexPage page;
    private final CodexPageState state = new CodexPageState();
    private final List<ItemFrame> frames = new ArrayList<>();
    private int width, height;

    public StandardCodexPageView(CodexPage page) {
        this.page = page;
    }

    public Optional<ResourceLocation> previewTarget() {
        return page.kind() == PageKind.STRUCTURE
                ? Optional.of(new ResourceLocation(page.reference()))
                : Optional.empty();
    }

    public CodexPageState state() {
        return state;
    }

    public void layout(int width, int height) {
        this.width = width;
        this.height = height;
        var lines = new ArrayList<String>();
        if (!page.body().isBlank())
            for (var paragraph : I18n.get(page.body()).split("<NL>", -1)) {
                Minecraft.getInstance()
                        .font
                        .getSplitter()
                        .splitLines(paragraph, width - 10, net.minecraft.network.chat.Style.EMPTY)
                        .forEach(line -> lines.add(line.getString()));
                lines.add("");
            }
        state.layout(lines);
    }

    public void draw(CodexDrawContext c, long tick) {
        frames.clear();
        state.frame();
        var mc = Minecraft.getInstance();
        c.text(
                Component.literal(mc.font.plainSubstrByWidth(I18n.get(page.title()), width - 6)),
                3,
                0,
                0xff583e29);
        int line = 18;
        for (var text : state.lines()) {
            if (line > height - 10) break;
            c.text(Component.literal(text), 3, line, 0xff342c28);
            line += 10;
        }
        switch (page.kind()) {
            case RECIPE, RECIPE_LIGHT, RECIPE_ALTAR -> recipe(c, tick);
            case STRUCTURE -> blueprint(c);
            case CELESTIAL -> sign(c);
            case BOON -> {
                var view = BoonMirror.view();
                c.text(
                        Component.literal("Lv " + view.level() + "   +" + view.availablePoints()),
                        8,
                        32,
                        0xff483969);
                c.text(Component.translatable("stellaeomphalos.codex.boons"), 8, 48, 0xff342c28);
            }
            case PROGRESS -> {
                var record = ClientKnowledgeCache.record();
                c.text(
                        Component.translatable(
                                "codex.stellaeomphalos.branch."
                                        + record.tier().name().toLowerCase(Locale.ROOT)),
                        8,
                        36,
                        0xff483969);
                c.text(Component.literal(record.knownSigns().size() + " / 16"), 8, 56, 0xff342c28);
                c.text(
                        Component.literal(
                                record.unlockedShards().size()
                                        + " / "
                                        + KnowledgeCatalog.SHARDS.all().size()),
                        8,
                        76,
                        0xff342c28);
            }
            case INDEX -> {
                int y = 24;
                for (var id :
                        ClientKnowledgeCache.record().unlockedShards().stream()
                                .sorted()
                                .limit(17)
                                .toList()) {
                    var shard = KnowledgeCatalog.SHARDS.find(id);
                    if (shard.isPresent())
                        c.text(Component.translatable(shard.get().nameKey()), 4, y, 0xff342c28);
                    y += 10;
                }
            }
            default -> {}
        }
    }

    private void recipe(CodexDrawContext c, long tick) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var recipe = mc.level.getRecipeManager().byKey(new ResourceLocation(page.reference()));
        if (recipe.isEmpty()) {
            c.text(Component.translatable("stellaeomphalos.codex.missing"), 3, 25, 0xff883333);
            return;
        }
        if (recipe.get() instanceof RecipeDisplayData data) {
            data.displaySlots()
                    .forEach(
                            (slot, spec) -> {
                                var choices = spec.displayStacks();
                                int choice =
                                        CodexPageState.candidate(
                                                tick, slot / 3, slot % 3, choices.size());
                                if (choice < 0) return;
                                int x =
                                        slot < 25
                                                ? (int) (AsterismMenuLayout.slotX(slot) * .72)
                                                : 4 + (slot % 8) * 20;
                                int y =
                                        slot < 25
                                                ? AsterismMenuLayout.slotY(slot)
                                                : 150 + (slot / 8) * 18;
                                item(c, choices.get(choice), x, y);
                            });
            int y = 169;
            for (var output : data.displayOutputs()) item(c, output, 76, y);
            c.text(Component.literal(data.displayDuration() + " ticks"), 5, 190, 0xff342c28);
            int textY = 201;
            for (var scalar : data.displayScalars().entrySet().stream().limit(1).toList())
                c.text(
                        Component.literal(scalar.getKey() + ": " + scalar.getValue()),
                        5,
                        textY,
                        0xff342c28);
        } else {
            var ingredients = recipe.get().getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                var candidates = ingredients.get(i).getItems();
                int index = CodexPageState.candidate(tick, i / 3, i % 3, candidates.length);
                if (index >= 0) item(c, candidates[index], 48 + i % 3 * 20, 36 + i / 3 * 20);
            }
            item(c, recipe.get().getResultItem(mc.level.registryAccess()), 68, 123);
        }
    }

    private void item(CodexDrawContext c, ItemStack stack, int x, int y) {
        c.rectangle(x - 1, y - 1, 18, 18, 0x22544236);
        c.item(stack, x, y);
        frames.add(new ItemFrame(x, y, stack.copy()));
    }

    private void blueprint(CodexDrawContext c) {
        var cells = ClientKnowledgeCache.blueprints().getList(page.reference(), Tag.TAG_COMPOUND);
        if (cells.isEmpty()) {
            c.text(Component.translatable("stellaeomphalos.codex.missing"), 3, 25, 0xff883333);
            return;
        }
        var mc = Minecraft.getInstance();
        var pose = c.graphics().pose();
        int extent = 1, maxY = 0;
        for (var entry : cells) {
            var tag = (CompoundTag) entry;
            extent =
                    Math.max(
                            extent,
                            Math.max(Math.abs(tag.getInt("X")), Math.abs(tag.getInt("Z"))) + 1);
            maxY = Math.max(maxY, tag.getInt("Y"));
        }
        float scale = Math.min(16, 60F / extent);
        pose.pushPose();
        pose.translate(c.x() + width / 2F, c.y() + 132, 160);
        pose.scale(scale, -scale, scale);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) state.rotation() + 35));
        for (var entry : cells) {
            var tag = (CompoundTag) entry;
            if (tag.getInt("Y") > state.slice()) continue;
            var block = BuiltInRegistries.BLOCK.get(new ResourceLocation(tag.getString("Block")));
            pose.pushPose();
            pose.translate(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
            mc.getBlockRenderer()
                    .renderSingleBlock(
                            block.defaultBlockState(),
                            pose,
                            c.graphics().bufferSource(),
                            15728880,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        c.graphics().flush();
        pose.popPose();
        c.text(
                Component.literal(
                        state.slice() == Integer.MAX_VALUE ? "Y: *" : "Y: " + state.slice()),
                4,
                189,
                0xff342c28);
        c.text(Component.translatable("stellaeomphalos.codex.preview"), 4, 203, 0xff6454a4);
    }

    private void sign(CodexDrawContext c) {
        var id = new ResourceLocation(page.reference());
        var stars = ClientKnowledgeCache.signs().getList(id.toString(), Tag.TAG_COMPOUND);
        if (!ClientKnowledgeCache.record().knownSigns().contains(id)) {
            c.text(Component.translatable("stellaeomphalos.codex.locked"), 3, 26, 0xff6a6259);
            return;
        }
        for (var value : stars) {
            var point = (CompoundTag) value;
            c.rectangle(20 + point.getInt("X") * 6, 35 + point.getInt("Y") * 6, 3, 3, 0xff7765ad);
        }
        c.text(Component.literal(id.getPath()), 5, 170, 0xff342c28);
    }

    public Optional<ItemStack> hitTest(double x, double y) {
        return frames.stream()
                .filter(f -> x >= f.x() && y >= f.y() && x < f.x() + 16 && y < f.y() + 16)
                .map(ItemFrame::stack)
                .findFirst();
    }

    public boolean drag(double dx) {
        return page.kind() == PageKind.STRUCTURE && state.rotate(dx);
    }

    public boolean scroll(double delta) {
        if (page.kind() != PageKind.STRUCTURE || !page.sliceable()) return false;
        int max =
                ClientKnowledgeCache.blueprints()
                        .getList(page.reference(), Tag.TAG_COMPOUND)
                        .stream()
                        .mapToInt(t -> ((CompoundTag) t).getInt("Y"))
                        .max()
                        .orElse(0);
        int current = state.slice() == Integer.MAX_VALUE ? max + 1 : state.slice();
        state.slice(Math.max(-64, Math.min(max + 1, current + (delta > 0 ? 1 : -1))));
        return true;
    }
}
