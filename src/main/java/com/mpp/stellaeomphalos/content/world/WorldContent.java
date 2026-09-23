package com.mpp.stellaeomphalos.content.world;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.block.*;
import com.mpp.stellaeomphalos.content.blockentity.rite.*;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.*;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.*;
import java.util.function.Supplier;

/** Registration catalogue; no world work or cross-registry resolution in static initializers. */
public final class WorldContent {
    public static final Map<String, RegistrationGuard<? extends Block>> BLOCKS =
            new LinkedHashMap<>();
    public static final Map<String, RegistrationGuard<? extends Item>> ITEMS =
            new LinkedHashMap<>();
    /**
     * 晶簇矿：单个方块 + {@link GeodeOreBlock.Variant} 表达 GEODE / ASTRAL 两个变体（§6.2.1.3）。
     *
     * <p>{@link #STAR_METAL_ORE} 不再是独立注册 id，而是同一方块的 ASTRAL 变体访问器，
     * 保留旧调用点的语义同时满足注册总账"一个 id 两个变体"的要求。
     */
    public static final RegistrationGuard<Block> GEODE_ORE =
            blockWithItem("geode_ore", GeodeOreBlock::new, com.mpp.stellaeomphalos.content.item.GeodeOreItem::new);

    /** ASTRAL 变体的注册守卫视图（与 {@link #GEODE_ORE} 同一方块，便于既有调用点过渡）。 */
    public static RegistrationGuard<Block> starMetalOre() {
        return GEODE_ORE;
    }

    /** 泉头方块（§6.2.1.2 `bore_head`）；物品有三个档位，方块只有一个 id。 */
    public static RegistrationGuard<Block> BORE_HEAD;

    /** 星辉矿（ASTRAL 变体）的方块状态。 */
    public static net.minecraft.world.level.block.state.BlockState astralOreState() {
        return GeodeOreBlock.astralState(GEODE_ORE.get());
    }

    public static final RegistrationGuard<Block>
            AQUAMARINE =
                    block(
                            "aquamarine_sand",
                            () ->
                                    new FallingBlock(
                                            BlockBehaviour.Properties.of()
                                                    .strength(0.5F)
                                                    .sound(SoundType.SAND))),
            ASTRAL_CRYSTAL = block("sky_crystal_cluster", () -> new CrystalClusterBlock(false)),
            GEM_CRYSTAL = block("prism_crystal_cluster", () -> new CrystalClusterBlock(true)),
            GLOW_FLOWER =
                    block(
                            "glowbloom",
                            () ->
                                    new FlowerBlock(
                                            () ->
                                                    net.minecraft.world.effect.MobEffects
                                                            .NIGHT_VISION,
                                            8,
                                            BlockBehaviour.Properties.of()
                                                    .noCollission()
                                                    .instabreak()
                                                    .lightLevel(s -> 9)));
    public static final RegistrationGuard<Block>
            PEDESTAL = block("rite_pedestal", RitePedestalBlock::new),
            SPRING = block("lumen_spring", LumenSpringBlock::new);
    public static final RegistrationGuard<BlockEntityType<RitePedestalBlockEntity>>
            PEDESTAL_ENTITY =
                    ModBlockEntities.ENTRIES.declare(
                            "rite_pedestal",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    RitePedestalBlockEntity::new, PEDESTAL.get())
                                            .build(null));
    public static final RegistrationGuard<BlockEntityType<LumenSpringBlockEntity>> SPRING_ENTITY =
            ModBlockEntities.ENTRIES.declare(
                    "lumen_spring",
                    () ->
                            BlockEntityType.Builder.of(LumenSpringBlockEntity::new, SPRING.get())
                                    .build(null));
    public static final RegistrationGuard<BlockEntityType<TechnicalBlockEntity>> TECH_ENTITY;

    static {
        for (String sound :
                List.of(
                        "structure_break",
                        "structure_formed",
                        "ritual_start",
                        "ritual_loop",
                        "rite_output",
                        "spring_draw"))
            ModSounds.ENTRIES.declare(
                    sound,
                    () ->
                            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                                    new net.minecraft.resources.ResourceLocation(
                                            Omphalos.MODID, sound)));
        for (String tier : List.of("crude", "polished", "resonant", "attuned"))
            block(
                    "rite_amplifier_" + tier,
                    () -> new Block(BlockBehaviour.Properties.of().strength(2, 6)));
        for (String technical :
                List.of(
                        "frame_shell",
                        "phase_barrier",
                        "proxy_foliage",
                        "mirage_shell",
                        "rupture_anchor",
                        "gate_node",
                        "gate_core",
                        "luminaire",
                        "rite_link",
                        "observatory",
                        "fountain",
                        "ore_regenerator",
                        "placeholder")) block(technical, () -> new TechnicalBlock(technical));
        // §6.2.1.2 / D-4：泉头是**一个**方块，两个模式变体由 EnumProperty 承载。
        BORE_HEAD = blockNoItem("bore_head", com.mpp.stellaeomphalos.content.block.BoreHeadBlock::new);
        for (var tier : com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier.values())
            ITEMS.put(
                    "bore_head_" + tier.getSerializedName(),
                    ModItems.ENTRIES.declare(
                            "bore_head_" + tier.getSerializedName(),
                            () ->
                                    new com.mpp.stellaeomphalos.content.item.BoreHeadItem(
                                            BORE_HEAD.get(),
                                            com.mpp.stellaeomphalos.content.block.BoreHeadBlock.BoreMode.LIQUID,
                                            tier)));
        blockNoItem("glow_mote", () -> new LightMoteBlock(false));
        blockNoItem("ephemeral_light", () -> new LightMoteBlock(true));
        TECH_ENTITY =
                ModBlockEntities.ENTRIES.declare(
                        "technical_frame",
                        () ->
                                BlockEntityType.Builder.of(
                                                TechnicalBlockEntity::new,
                                                BLOCKS.entrySet().stream()
                                                        .filter(
                                                                e ->
                                                                        Set.of(
                                                                                        "frame_shell",
                                                                                        "mirage_shell",
                                                                                        "proxy_foliage",
                                                                                        "gate_core",
                                                                                        "luminaire",
                                                                                        "rite_link",
                                                                                        "observatory",
                                                                                        "fountain",
                                                                                        "ore_regenerator")
                                                                                .contains(
                                                                                        e.getKey()))
                                                        .map(e -> e.getValue().get())
                                                        .toArray(Block[]::new))
                                        .build(null));
        for (String material :
                List.of(
                        "aquamarine",
                        "astral_ingot",
                        "star_dust",
                        "prism_shard"))
            ITEMS.put(
                    material,
                    ModItems.ENTRIES.declare(material, () -> new Item(new Item.Properties())));
        // 《方块物品实体完整清单》§6.2.2.1：材料族的剩余两项（共鸣宝石、羊皮纸）与既有材料合并为六个独立物品。
        for (String material : List.of("resonance_gem", "parchment"))
            ITEMS.put(
                    material,
                    ModItems.ENTRIES.declare(material, () -> new Item(new Item.Properties())));
        ITEMS.put("astrolabe", ModItems.ENTRIES.declare("astrolabe", () -> new AstrolabeItem()));
        ModCreativeTabs.ENTRIES.declare(
                "world_and_rites",
                () ->
                        CreativeModeTab.builder()
                                .title(
                                        Component.translatable(
                                                "itemGroup.stellaeomphalos.world_and_rites"))
                                .icon(() -> new ItemStack(PEDESTAL.get()))
                                .displayItems(
                                        (p, out) ->
                                                {
                                                    ITEMS.forEach((name, item) -> { if (!name.equals("placeholder")) out.accept(item.get()); });
                                                    com.mpp.stellaeomphalos.content.block.DecorContent
                                                            .ITEM_BY_ID
                                                            .values()
                                                            .forEach(item -> out.accept(item.get()));
                                                    out.accept(
                                                            new ItemStack(
                                                                    com.mpp.stellaeomphalos.content.block
                                                                            .MachineContent.COSMETIC_ROCK_ITEM
                                                                            .get()));
                                                    out.accept(
                                                            new ItemStack(
                                                                    com.mpp.stellaeomphalos.content.block
                                                                            .MachineContent
                                                                            .CONSTELLATION_FRAME_ITEM
                                                                            .get()));
                                                    com.mpp.stellaeomphalos.content.block
                                                            .MachineContent.MACHINES
                                                            .forEach(
                                                                    machine ->
                                                                            out.accept(
                                                                                    new ItemStack(
                                                                                            machine
                                                                                                    .get())));
                                                    // 《方块物品实体完整清单》§6.2.2：物品总账的其余条目。
                                                    // `rosewood_bow` 按规划**不进创造页**（遗留物品）。
                                                    com.mpp.stellaeomphalos.content.item.CatalogItems
                                                            .ITEMS
                                                            .forEach(
                                                                    (id, item) -> {
                                                                        if (id.equals("rosewood_bow"))
                                                                            return;
                                                                        out.accept(
                                                                                new ItemStack(
                                                                                        item.get()));
                                                                    });
                                                    for (String id : List.of("collector", "lumen_relay", "lumen_battery",
                                                            "molten_lumen_bucket", "geode_shard", "infused_log"))
                                                        out.accept(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                                                                new net.minecraft.resources.ResourceLocation(Omphalos.MODID, id)));
                                                })
                                .build());
    }

    public static final RegistrationGuard<
                    net.minecraft.world.entity.EntityType<
                            com.mpp.stellaeomphalos.content.entity.StarfallEntity>>
            STARFALL =
                    ModEntities.ENTRIES.declare(
                            "starfall",
                            () ->
                                    net.minecraft.world.entity.EntityType.Builder
                                            .<com.mpp.stellaeomphalos.content.entity.StarfallEntity>
                                                    of(
                                                            com.mpp.stellaeomphalos.content.entity
                                                                            .StarfallEntity
                                                                    ::new,
                                                            net.minecraft.world.entity.MobCategory
                                                                    .MISC)
                                            .sized(0.25F, 0.25F)
                                            .clientTrackingRange(128)
                                            .updateInterval(20)
                                            .build("stellaeomphalos:starfall"));

    private WorldContent() {}

    public static void initialize() {}

    private static RegistrationGuard<Block> block(String id, Supplier<Block> factory) {
        var b = ModBlocks.ENTRIES.declare(id, factory);
        BLOCKS.put(id, b);
        ITEMS.put(
                id,
                ModItems.ENTRIES.declare(id, () -> new BlockItem(b.get(), new Item.Properties())));
        return b;
    }

    /** 不可获得的技术方块：只注册方块本体，不产生物品与创造页条目。 */
    private static RegistrationGuard<Block> blockNoItem(String id, Supplier<Block> factory) {
        var b = ModBlocks.ENTRIES.declare(id, factory);
        BLOCKS.put(id, b);
        return b;
    }

    /** 需要专用物品形态的方块：物品构造器由调用方给出（如晶簇矿的变体保持）。 */
    private static <I extends net.minecraft.world.item.Item> RegistrationGuard<Block> blockWithItem(
            String id, Supplier<Block> factory, java.util.function.Function<Block, I> itemFactory) {
        var b = ModBlocks.ENTRIES.declare(id, factory);
        BLOCKS.put(id, b);
        ITEMS.put(id, ModItems.ENTRIES.declare(id, () -> itemFactory.apply(b.get())));
        return b;
    }
}
