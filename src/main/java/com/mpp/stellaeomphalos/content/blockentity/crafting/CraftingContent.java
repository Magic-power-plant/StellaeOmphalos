package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.*;
import com.mpp.stellaeomphalos.crafting.altar.menu.SignFocusProvider;
import com.mpp.stellaeomphalos.crafting.altar.recipe.*;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

import java.util.*;

@Mod.EventBusSubscriber(modid = Omphalos.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CraftingContent {
    public static final EnumProperty<AsterismTier> TIER =
            EnumProperty.create("tier", AsterismTier.class);
    public static final RegistrationGuard<MachineBlock> ALTAR = block("asterism_altar", "asterism");
    public static final RegistrationGuard<MachineBlock>
            INFUSER = block("lumen_infuser", "lumen_infuser"),
            GRINDWHEEL = block("grindwheel", "grindwheel"),
            WELL = block("lumen_well", "lumen_well"),
            CHALICE = block("lumen_chalice", "lumen_chalice"),
            RELAY = block("crafting_relay", "crafting_relay");
    public static final RegistrationGuard<BlockEntityType<AsterismAltarBlockEntity>> ALTAR_ENTITY =
            ModBlockEntities.ENTRIES.declare(
                    "asterism_altar",
                    () ->
                            BlockEntityType.Builder.of(AsterismAltarBlockEntity::new, ALTAR.get())
                                    .build(null));
    public static final RegistrationGuard<BlockEntityType<LumenInfuserBlockEntity>> INFUSER_ENTITY =
            ModBlockEntities.ENTRIES.declare(
                    "lumen_infuser",
                    () ->
                            BlockEntityType.Builder.of(LumenInfuserBlockEntity::new, INFUSER.get())
                                    .build(null));
    public static final RegistrationGuard<BlockEntityType<GrindwheelBlockEntity>>
            GRINDWHEEL_ENTITY =
                    ModBlockEntities.ENTRIES.declare(
                            "grindwheel",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    GrindwheelBlockEntity::new, GRINDWHEEL.get())
                                            .build(null));
    public static final RegistrationGuard<BlockEntityType<LumenWellBlockEntity>> WELL_ENTITY =
            ModBlockEntities.ENTRIES.declare(
                    "lumen_well",
                    () ->
                            BlockEntityType.Builder.of(LumenWellBlockEntity::new, WELL.get())
                                    .build(null));
    public static final RegistrationGuard<BlockEntityType<LumenChaliceBlockEntity>> CHALICE_ENTITY =
            ModBlockEntities.ENTRIES.declare(
                    "lumen_chalice",
                    () ->
                            BlockEntityType.Builder.of(LumenChaliceBlockEntity::new, CHALICE.get())
                                    .build(null));
    public static final RegistrationGuard<BlockEntityType<CraftingRelayBlockEntity>> RELAY_ENTITY =
            ModBlockEntities.ENTRIES.declare(
                    "crafting_relay",
                    () ->
                            BlockEntityType.Builder.of(CraftingRelayBlockEntity::new, RELAY.get())
                                    .build(null));
    public static final RegistrationGuard<MachineBlock> TRANSMUTER =
            block("light_transmuter", "light_transmuter");
    public static final RegistrationGuard<BlockEntityType<TransmutationCoreBlockEntity>>
            TRANSMUTER_ENTITY =
                    ModBlockEntities.ENTRIES.declare(
                            "light_transmuter",
                            () ->
                                    BlockEntityType.Builder.of(
                                                    TransmutationCoreBlockEntity::new,
                                                    TRANSMUTER.get())
                                            .build(null));
    public static final Map<String, RegistrationGuard<? extends Item>> ITEMS =
            new LinkedHashMap<>();

    static {
        ITEMS.put(
                "lumen_flask",
                ModItems.ENTRIES.declare(
                        "lumen_flask", com.mpp.stellaeomphalos.content.item.LumenFlaskItem::new));
        for (var tier : AsterismTier.values())
            ITEMS.put(
                    "asterism_altar_" + tier.getSerializedName(),
                    ModItems.ENTRIES.declare(
                            "asterism_altar_" + tier.getSerializedName(),
                            () -> new AltarItem(tier)));
        for (var block : List.of(INFUSER, GRINDWHEEL, WELL, CHALICE, RELAY, TRANSMUTER))
            ITEMS.put(
                    block.id().getPath(),
                    ModItems.ENTRIES.declare(
                            block.id().getPath(),
                            () -> new BlockItem(block.get(), new Item.Properties())));
        ITEMS.put("sign_focus", ModItems.ENTRIES.declare("sign_focus", FocusItem::new));
        for (String name :
                List.of(
                        "raw_crystal",
                        "resonant_crystal",
                        "star_lens",
                        "prism_lens",
                        "star_sextant",
                        "illumination_wand",
                        "star_mantle",
                        "sign_paper",
                        "conversion_star",
                        "enchant_charm",
                        "drill_head",
                        "resonator",
                        "ritual_base",
                        "charged_tool",
                        "collector_crystal",
                        "crystal_pickaxe",
                        "crystal_axe",
                        "crystal_shovel",
                        "crystal_sword"))
            ITEMS.put(
                    name,
                    ModItems.ENTRIES.declare(
                            name,
                            () ->
                                    name.equals("star_mantle")
                                            ? new com.mpp.stellaeomphalos.content.item
                                                    .StarMantleItem()
                                            : name.equals("crystal_sword")
                                                    ? new SwordItem(
                                                            Tiers.DIAMOND,
                                                            3,
                                                            -2.4F,
                                                            new Item.Properties())
                                                    : new Item(
                                                            new Item.Properties()
                                                                    .stacksTo(
                                                                            name.contains("crystal")
                                                                                            || name
                                                                                                    .contains(
                                                                                                            "tool")
                                                                                            || name
                                                                                                    .contains(
                                                                                                            "wand")
                                                                                            || name
                                                                                                    .contains(
                                                                                                            "mantle")
                                                                                    ? 1
                                                                                    : 64))));
        ModCreativeTabs.ENTRIES.declare(
                "crafting",
                () ->
                        CreativeModeTab.builder()
                                .title(Component.translatable("itemGroup.stellaeomphalos.crafting"))
                                .icon(
                                        () ->
                                                new ItemStack(
                                                        ITEMS.get("asterism_altar_discovery")
                                                                .get()))
                                .displayItems(
                                        (parameters, output) ->
                                                ITEMS.forEach(
                                                        (id, item) -> {
                                                            if (!id.equals(
                                                                    "asterism_altar_radiance"))
                                                                output.accept(item.get());
                                                        }))
                                .build());
        RecipeCatalog.initialize();
        com.mpp.stellaeomphalos.crafting.altar.menu.MachineMenus.initialize();
    }

    public static final RegistrationGuard<net.minecraft.sounds.SoundEvent> CRAFT_LOOP =
            sound("craft_loop");
    public static final RegistrationGuard<net.minecraft.sounds.SoundEvent> CRAFT_FINISH =
            sound("craft_finish");
    public static final RegistrationGuard<net.minecraft.sounds.SoundEvent> INFUSION_BUBBLE =
            sound("infusion_bubble");
    public static final RegistrationGuard<net.minecraft.sounds.SoundEvent> GRINDWHEEL_SPIN =
            sound("grindwheel_spin");

    private static RegistrationGuard<net.minecraft.sounds.SoundEvent> sound(String name) {
        return ModSounds.ENTRIES.declare(
                name,
                () ->
                        net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                                new ResourceLocation(Omphalos.MODID, name)));
    }

    private CraftingContent() {}

    private static RegistrationGuard<MachineBlock> block(String id, String kind) {
        return ModBlocks.ENTRIES.declare(id, () -> new MachineBlock(kind));
    }

    @SubscribeEvent
    public static void data(GatherDataEvent event) {
        event.getGenerator()
                .addProvider(
                        event.includeServer() || event.includeClient(),
                        new CraftingDataProvider(event.getGenerator().getPackOutput()));
    }

    public static final class AltarItem extends BlockItem {
        private final AsterismTier tier;

        AltarItem(AsterismTier tier) {
            super(ALTAR.get(), new Item.Properties());
            this.tier = tier;
        }

        @Override
        public String getDescriptionId() {
            return "item.stellaeomphalos.asterism_altar_" + tier.getSerializedName();
        }

        @Override
        protected BlockState getPlacementState(BlockPlaceContext context) {
            var state = super.getPlacementState(context);
            return state == null ? null : state.setValue(TIER, tier);
        }
    }

    public static final class FocusItem extends Item implements SignFocusProvider {
        FocusItem() {
            super(new Item.Properties().stacksTo(1));
        }

        public Optional<ResourceLocation> sign(ItemStack stack) {
            return stack.hasTag()
                    ? Optional.ofNullable(
                            ResourceLocation.tryParse(stack.getTag().getString("SignId")))
                    : Optional.empty();
        }
    }

    public static final class MachineBlock extends Block implements EntityBlock {
        private final String kind;

        MachineBlock(String kind) {
            super(BlockBehaviour.Properties.of().strength(3.0F));
            this.kind = kind;
            registerDefaultState(stateDefinition.any().setValue(TIER, AsterismTier.DISCOVERY));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(TIER);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return switch (kind) {
                case "asterism" -> new AsterismAltarBlockEntity(pos, state);
                case "lumen_infuser" -> new LumenInfuserBlockEntity(pos, state);
                case "grindwheel" -> new GrindwheelBlockEntity(pos, state);
                case "lumen_well" -> new LumenWellBlockEntity(pos, state);
                case "lumen_chalice" -> new LumenChaliceBlockEntity(pos, state);
                case "light_transmuter" -> new TransmutationCoreBlockEntity(pos, state);
                default -> new CraftingRelayBlockEntity(pos, state);
            };
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide
                    ? null
                    : (world, pos, block, entity) -> {
                        if (entity instanceof AbstractCraftingMachine machine) machine.serverTick();
                    };
        }

        @Override
        public void neighborChanged(
                BlockState state,
                Level level,
                BlockPos pos,
                Block block,
                BlockPos from,
                boolean moving) {
            if (level.getBlockEntity(pos) instanceof AbstractCraftingMachine machine)
                machine.onNeighborChanged(level, from);
        }

        @Override
        public InteractionResult use(
                BlockState state,
                Level level,
                BlockPos pos,
                Player player,
                InteractionHand hand,
                BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof AbstractCraftingMachine machine))
                return InteractionResult.PASS;
            if (kind.equals("lumen_chalice") || kind.equals("lumen_well")) {
                if (FluidUtil.interactWithFluidHandler(
                        player, hand, level, pos, hit.getDirection()))
                    return InteractionResult.sidedSuccess(level.isClientSide);
                if (kind.equals("lumen_chalice")) return InteractionResult.SUCCESS;
            }
            if (kind.equals("light_transmuter")) return InteractionResult.SUCCESS;
            if (kind.equals("crafting_relay")) {
                if (!level.isClientSide) {
                    var held = player.getItemInHand(hand);
                    if (machine.inventory().getStackInSlot(0).isEmpty() && !held.isEmpty()) {
                        machine.inventory().setStackInSlot(0, held.split(1));
                    } else if (held.isEmpty()) {
                        player.setItemInHand(hand, machine.inventory().getStackInSlot(0));
                        machine.inventory().setStackInSlot(0, ItemStack.EMPTY);
                    }
                    machine.setChanged();
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (player instanceof ServerPlayer server)
                NetworkHooks.openScreen(
                        server,
                        machine,
                        buf -> {
                            buf.writeBlockPos(pos);
                            buf.writeUtf(machine.tier().name(), 32);
                        });
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void onRemove(
                BlockState old, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (old.getBlock() != next.getBlock()
                    && level.getBlockEntity(pos) instanceof AbstractCraftingMachine machine)
                machine.dropContents();
            super.onRemove(old, level, pos, next, moving);
        }

        @Override
        public ItemStack getCloneItemStack(
                net.minecraft.world.level.BlockGetter world, BlockPos pos, BlockState state) {
            return new ItemStack(
                    ITEMS.get(
                                    kind.equals("asterism")
                                            ? "asterism_altar_"
                                                    + state.getValue(TIER).getSerializedName()
                                            : kind)
                            .get());
        }
    }
}
