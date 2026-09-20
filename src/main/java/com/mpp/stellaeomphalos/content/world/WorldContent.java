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
    public static final RegistrationGuard<Block> STONE =
            block("stone_set", () -> new StoneSetBlock(false));
    public static final RegistrationGuard<Block> GEODE_ORE = block("geode_ore", GeodeOreBlock::new),
            STAR_METAL_ORE =
                    block(
                            "star_metal_ore",
                            () ->
                                    new Block(
                                            BlockBehaviour.Properties.of()
                                                    .strength(3, 9)
                                                    .requiresCorrectToolForDrops())),
            AQUAMARINE =
                    block(
                            "aquamarine_sand_ore",
                            () ->
                                    new FallingBlock(
                                            BlockBehaviour.Properties.of()
                                                    .strength(0.5F)
                                                    .sound(SoundType.SAND))),
            ASTRAL_CRYSTAL = block("astral_crystal", () -> new CrystalClusterBlock(false)),
            GEM_CRYSTAL = block("gem_crystal", () -> new CrystalClusterBlock(true)),
            GLOW_FLOWER =
                    block(
                            "glow_flower",
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
        for (String family : List.of("stone_set", "dark_stone_set", "infused_wood_set"))
            for (String variant :
                    List.of("raw", "bricks", "pillar", "arch", "chiseled", "engraved", "runed")) {
                String id = variant.equals("raw") ? family : family + "_" + variant;
                if (!BLOCKS.containsKey(id))
                    block(id, () -> new StoneSetBlock(variant.equals("pillar")));
            }
        for (String tier : List.of("crude", "polished", "resonant", "attuned"))
            block(
                    "rite_amplifier_" + tier,
                    () -> new Block(BlockBehaviour.Properties.of().strength(2, 6)));
        for (String technical :
                List.of(
                        "frame_shell",
                        "ward_block",
                        "mimic_block",
                        "flare_light",
                        "gate_node",
                        "gate_core",
                        "world_lamp",
                        "rite_link",
                        "observatory",
                        "bore_core",
                        "bore_head_stone",
                        "bore_head_iron",
                        "bore_head_diamond",
                        "mineral_regenerator",
                        "placeholder")) block(technical, () -> new TechnicalBlock(technical));
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
                                                                                        "mimic_block",
                                                                                        "gate_core",
                                                                                        "world_lamp",
                                                                                        "rite_link",
                                                                                        "observatory",
                                                                                        "bore_core",
                                                                                        "mineral_regenerator")
                                                                                .contains(
                                                                                        e.getKey()))
                                                        .map(e -> e.getValue().get())
                                                        .toArray(Block[]::new))
                                        .build(null));
        for (String material :
                List.of(
                        "aquamarine",
                        "star_metal_ingot",
                        "astral_crystal_shard",
                        "gem_crystal_shard"))
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
                                                ITEMS.values().forEach(i -> out.accept(i.get())))
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
}
