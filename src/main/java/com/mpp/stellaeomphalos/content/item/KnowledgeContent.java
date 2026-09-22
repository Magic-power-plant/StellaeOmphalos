package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class KnowledgeContent {
    public static final RegistrationGuard<CodexItem> CODEX =
            ModItems.ENTRIES.declare("codex", CodexItem::new);
    public static final RegistrationGuard<LoreShardItem> SHARD =
            ModItems.ENTRIES.declare("lore_shard", LoreShardItem::new);
    public static final RegistrationGuard<LoreCapsuleItem> CAPSULE =
            ModItems.ENTRIES.declare("lore_capsule", LoreCapsuleItem::new);
    public static final RegistrationGuard<LoreScrollItem> SCROLL =
            ModItems.ENTRIES.declare("lore_scroll", LoreScrollItem::new);

    private KnowledgeContent() {}

    public static void initialize() {
        ModCreativeTabs.ENTRIES.declare(
                "knowledge",
                () ->
                        CreativeModeTab.builder()
                                .title(
                                        Component.translatable(
                                                "itemGroup.stellaeomphalos.knowledge"))
                                .icon(() -> new ItemStack(CODEX.get()))
                                .displayItems(
                                        (parameters, output) -> {
                                            output.accept(CODEX.get());
                                            output.accept(SHARD.get());
                                            output.accept(CAPSULE.get());
                                            output.accept(SCROLL.get());
                                        })
                                .build());
    }
}
