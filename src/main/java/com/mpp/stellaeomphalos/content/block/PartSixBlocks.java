package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.blockentity.SyncedBlockEntity;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModBlockEntities;
import com.mpp.stellaeomphalos.core.registry.ModBlocks;
import com.mpp.stellaeomphalos.core.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

/**
 * Part-6 §6.2.1.2 / §6.2.1.5 的机器与星辉网络方块注册：观星镜、星图台、林木信标、天体仪、
 * 集束透镜、折射棱镜、共鸣坛、星构框架件与装饰岩块。
 *
 * <p>方块实体状态字段按 §6.4.2 的字段表实现（键统一 PascalCase，遵循项目 NBT 规范）；
 * 渲染与音效的最终表现归 Part-7。
 */
public final class PartSixBlocks {

    public static final net.minecraft.world.level.block.state.properties.IntegerProperty ROTATION =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("rotation", 0, 7);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final RegistrationGuard<Block> COSMETIC_ROCK =
            ModBlocks.ENTRIES.declare(
                    "cosmetic_rock",
                    () ->
                            new Block(
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.STONE)
                                            .strength(2.0F, 20.0F)
                                            .requiresCorrectToolForDrops()));
    public static final RegistrationGuard<Block> CONSTELLATION_FRAME =
            ModBlocks.ENTRIES.declare(
                    "constellation_frame",
                    () ->
                            new Block(
                                    BlockBehaviour.Properties.of()
                                            .mapColor(MapColor.QUARTZ)
                                            .strength(2.0F, 6.0F)
                                            .sound(SoundType.STONE)
                                            .requiresCorrectToolForDrops()));

    /** §6.2.2.7：两个普通方块的物品形态。 */
    public static final RegistrationGuard<BlockItem> COSMETIC_ROCK_ITEM =
            ModItems.ENTRIES.declare(
                    "cosmetic_rock",
                    () -> new BlockItem(COSMETIC_ROCK.get(), new Item.Properties()));

    public static final RegistrationGuard<BlockItem> CONSTELLATION_FRAME_ITEM =
            ModItems.ENTRIES.declare(
                    "constellation_frame",
                    () -> new BlockItem(CONSTELLATION_FRAME.get(), new Item.Properties()));

    public static final RegistrationGuard<MachineBlock> SPYGLASS =
            machine("spyglass", MachineKind.SPYGLASS);
    public static final RegistrationGuard<MachineBlock> STAR_CHART_TABLE =
            machine("star_chart_table", MachineKind.STAR_CHART_TABLE);
    public static final RegistrationGuard<MachineBlock> GROVE_BEACON =
            machine("grove_beacon", MachineKind.GROVE_BEACON);
    public static final RegistrationGuard<MachineBlock> CELESTIAL_ORRERY =
            machine("celestial_orrery", MachineKind.CELESTIAL_ORRERY);
    public static final RegistrationGuard<MachineBlock> BEAM_LENS =
            machine("beam_lens", MachineKind.BEAM_LENS);
    public static final RegistrationGuard<MachineBlock> BEAM_PRISM =
            machine("beam_prism", MachineKind.BEAM_PRISM);
    public static final RegistrationGuard<MachineBlock> RESONANCE_ALTAR =
            machine("resonance_altar", MachineKind.RESONANCE_ALTAR);

    public static final RegistrationGuard<BlockEntityType<MachineBlockEntity>> SPYGLASS_ENTITY =
            entity("spyglass", SPYGLASS);
    public static final RegistrationGuard<BlockEntityType<MachineBlockEntity>> STAR_CHART_ENTITY =
            entity("star_chart_table", STAR_CHART_TABLE);
    public static final RegistrationGuard<BlockEntityType<MachineBlockEntity>> GROVE_BEACON_ENTITY =
            entity("grove_beacon", GROVE_BEACON);
    public static final RegistrationGuard<BlockEntityType<MachineBlockEntity>> ORRERY_ENTITY =
            entity("celestial_orrery", CELESTIAL_ORRERY);
    public static final RegistrationGuard<BlockEntityType<MachineBlockEntity>> BEAM_LENS_ENTITY =
            entity("beam_lens", BEAM_LENS);
    public static final RegistrationGuard<BlockEntityType<MachineBlockEntity>> BEAM_PRISM_ENTITY =
            entity("beam_prism", BEAM_PRISM);
    public static final RegistrationGuard<BlockEntityType<MachineBlockEntity>> RESONANCE_ENTITY =
            entity("resonance_altar", RESONANCE_ALTAR);

    public static final List<RegistrationGuard<MachineBlock>> MACHINES =
            List.of(
                    SPYGLASS,
                    STAR_CHART_TABLE,
                    GROVE_BEACON,
                    CELESTIAL_ORRERY,
                    BEAM_LENS,
                    BEAM_PRISM,
                    RESONANCE_ALTAR);

    static {
        for (var kind : MachineKind.values())
            for (String id : kind.langKeys())
                com.mpp.stellaeomphalos.data.loader.FoundationDataProvider.language(
                        "block." + Omphalos.MODID + "." + id,
                        kind.english(),
                        kind.chinese());
        com.mpp.stellaeomphalos.data.loader.FoundationDataProvider.language(
                "block." + Omphalos.MODID + ".cosmetic_rock", "Cosmetic Rock", "装饰岩块");
        com.mpp.stellaeomphalos.data.loader.FoundationDataProvider.language(
                "block." + Omphalos.MODID + ".constellation_frame", "Constellation Frame", "星构框架");
    }

    private PartSixBlocks() {}

    public static void initialize() {}

    private static RegistrationGuard<MachineBlock> machine(String id, MachineKind kind) {
        var guard = ModBlocks.ENTRIES.declare(id, () -> new MachineBlock(kind));
        ModItems.ENTRIES.declare(
                id, () -> new BlockItem(guard.get(), new Item.Properties()));
        return guard;
    }

    private static RegistrationGuard<BlockEntityType<MachineBlockEntity>> entity(
            String id, RegistrationGuard<MachineBlock> block) {
        return ModBlockEntities.ENTRIES.declare(
                id,
                () ->
                        BlockEntityType.Builder.of(
                                        (pos, state) ->
                                                new MachineBlockEntity(
                                                        entityTypeOf(id), pos, state),
                                        block.get())
                                .build(null));
    }

    /** 注册期解析自身类型：延迟到首次构造，避免静态初始化环。 */
    private static BlockEntityType<MachineBlockEntity> entityTypeOf(String id) {
        @SuppressWarnings("unchecked")
        var type =
                (BlockEntityType<MachineBlockEntity>)
                        ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(
                                new ResourceLocation(Omphalos.MODID, id));
        if (type == null) throw new IllegalStateException("Missing block entity type " + id);
        return type;
    }

    /** §6.2.1.2 / §6.2.1.5 的机器种类。 */
    public enum MachineKind {
        SPYGLASS("Telescope", "观星镜"),
        STAR_CHART_TABLE("Star Chart Table", "星图台"),
        GROVE_BEACON("Grove Beacon", "林木信标"),
        CELESTIAL_ORRERY("Celestial Orrery", "天体仪"),
        BEAM_LENS("Beam Lens", "集束透镜"),
        BEAM_PRISM("Beam Prism", "折射棱镜"),
        RESONANCE_ALTAR("Resonance Altar", "共鸣坛");

        private final String english;
        private final String chinese;

        MachineKind(String english, String chinese) {
            this.english = english;
            this.chinese = chinese;
        }

        public String english() {
            return english;
        }

        public String chinese() {
            return chinese;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public List<String> langKeys() {
            return List.of(
                    switch (this) {
                        case SPYGLASS -> "spyglass";
                        case STAR_CHART_TABLE -> "star_chart_table";
                        case GROVE_BEACON -> "grove_beacon";
                        case CELESTIAL_ORRERY -> "celestial_orrery";
                        case BEAM_LENS -> "beam_lens";
                        case BEAM_PRISM -> "beam_prism";
                        case RESONANCE_ALTAR -> "resonance_altar";
                    });
        }
    }

    /** 机器方块：按 kind 分派交互与两格高的观星镜占位逻辑。 */
    public static final class MachineBlock extends Block implements EntityBlock {
        private final MachineKind kind;

        MachineBlock(MachineKind kind) {
            super(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.QUARTZ)
                            .strength(3.0F, 6.0F)
                            .noOcclusion()
                            .sound(SoundType.STONE));
            this.kind = kind;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROTATION, 0));
        }

        public MachineKind kind() {
            return kind;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING, ROTATION);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            if (kind == MachineKind.SPYGLASS) {
                var above = context.getClickedPos().above();
                if (!context.getLevel().getBlockState(above).canBeReplaced(context)) return null;
            }
            return defaultBlockState()
                    .setValue(
                            FACING,
                            context.getHorizontalDirection().getOpposite())
                    .setValue(ROTATION, Math.floorMod(Math.round(context.getRotation() / 45F), 8));
        }

        @Override
        public void setPlacedBy(
                Level level,
                BlockPos pos,
                BlockState state,
                LivingEntity placer,
                ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level.isClientSide) return;
            if (kind == MachineKind.SPYGLASS) {
                // §6.6.1：观星镜为两格高，上格由 frame_shell 补齐。
                var shell = com.mpp.stellaeomphalos.content.world.WorldContent.BLOCKS.get("frame_shell");
                if (shell != null)
                    level.setBlock(pos.above(), shell.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            if ((kind == MachineKind.GROVE_BEACON))
                if (level.getBlockEntity(pos) instanceof MachineBlockEntity be
                        && placer instanceof Player player
                        && !(player instanceof net.minecraftforge.common.util.FakePlayer))
                    be.setOwner(player.getUUID());
        }

        @Override
        public void neighborChanged(
                BlockState state,
                Level level,
                BlockPos pos,
                Block neighbor,
                BlockPos from,
                boolean moving) {
            if (level.isClientSide || kind != MachineKind.SPYGLASS) return;
            if (level.getBlockState(pos.above()).isAir()) level.removeBlock(pos, false);
        }

        @Override
        public void onRemove(
                BlockState old, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (old.getBlock() != next.getBlock()
                    && level.getBlockEntity(pos) instanceof MachineBlockEntity be)
                be.dropContents(level, pos);
            super.onRemove(old, level, pos, next, moving);
        }

        @Override
        public InteractionResult use(
                BlockState state,
                Level level,
                BlockPos pos,
                Player player,
                InteractionHand hand,
                BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity be))
                return InteractionResult.PASS;
            var held = player.getItemInHand(hand);
            return switch (kind) {
                case SPYGLASS -> {
                    // §6.2.9：观星镜容器承载朝向；实际的朝向更新包归 Part-8，屏幕归 Part-7。
                    openStation(
                            level,
                            pos,
                            player,
                            com.mpp.stellaeomphalos.content.menu.PartSixMenus.SpyglassMenu::new);
                    yield InteractionResult.sidedSuccess(level.isClientSide);
                }
                case STAR_CHART_TABLE -> {
                    var result = be.interactStarChart(level, pos, player, hand, held);
                    // §6.6.1：手持不可放置物时不接管，交给星图台容器。
                    if (result == InteractionResult.PASS) {
                        openStation(
                                    level,
                                    pos,
                                    player,
                                    com.mpp.stellaeomphalos.content.menu.PartSixMenus.StarChartTableMenu::new);
                        yield InteractionResult.sidedSuccess(level.isClientSide);
                    }
                    yield result;
                }
                case BEAM_LENS, BEAM_PRISM -> {
                    var color = lensColor(held);
                    if (color == null) yield InteractionResult.PASS;
                    if (!level.isClientSide) {
                        be.setLensColor(color);
                        if (!player.getAbilities().instabuild) held.shrink(1);
                        var sound =
                                ForgeRegistries.SOUND_EVENTS.getValue(
                                        new ResourceLocation(Omphalos.MODID, "clip_switch"));
                        if (sound != null)
                            level.playSound(null, pos, sound, SoundSource.PLAYERS, 0.6F, 1.0F);
                    }
                    yield InteractionResult.sidedSuccess(level.isClientSide);
                }
                case RESONANCE_ALTAR -> InteractionResult.sidedSuccess(level.isClientSide);
                default -> InteractionResult.PASS;
            };
        }

        /** 打开 Part-6 的定点容器；服务端开屏、客户端只回成功。 */
        static void openStation(Level level, BlockPos pos, Player player, StationFactory factory) {
            if (level.isClientSide) return;
            if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return;
            net.minecraftforge.network.NetworkHooks.openScreen(
                    server,
                    new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, ignored) -> factory.create(id, inventory, pos),
                            net.minecraft.network.chat.Component.translatable(
                                    "container.stellaeomphalos.station")),
                    buffer -> com.mpp.stellaeomphalos.content.menu.PartSixMenus.writePos(buffer, pos));
        }

        /** 容器构造点：同一个 lambda 同时服务服务端与客户端。 */
        @FunctionalInterface
        interface StationFactory {
            net.minecraft.world.inventory.AbstractContainerMenu create(
                    int id, net.minecraft.world.entity.player.Inventory inventory, BlockPos pos);
        }

        private static String lensColor(ItemStack stack) {
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !id.getNamespace().equals(Omphalos.MODID)) return null;
            if (!id.getPath().startsWith("prism_lens")) return null;
            return id.getPath();
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MachineBlockEntity(entityTypeOf(kind.id()), pos, state);
        }

        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
                Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide
                    ? null
                    : (world, pos, blockState, be) -> {
                        if (be instanceof MachineBlockEntity machine) machine.serverTick();
                    };
        }
    }

    /** §6.4.2 的机器状态字段载体：按 kind 使用相应字段，未使用的字段不写入 NBT。 */
    public static final class MachineBlockEntity extends SyncedBlockEntity {
        private final ItemStackHandler inventory =
                new ItemStackHandler(2) {
                    @Override
                    protected void onContentsChanged(int slot) {
                        markClientDirty();
                    }
                };
        private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);

        private int rotation;
        private UUID owner;
        private double charge;
        private int runTick;
        private int parchment;
        private String lensColor = "";
        private int mode;
        private String foundSign = "";

        public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public int rotation() {
            return rotation;
        }

        public void setRotation(int value) {
            rotation = Math.floorMod(value, 8);
            if (level != null && !level.isClientSide && getBlockState().hasProperty(ROTATION))
                level.setBlock(worldPosition, getBlockState().setValue(ROTATION, rotation), Block.UPDATE_ALL);
            markClientDirty();
        }

        public void setOwner(UUID value) {
            owner = value;
            markClientDirty();
        }

        public UUID owner() {
            return owner;
        }

        public double charge() {
            return charge;
        }

        public String lensColor() {
            return lensColor.isEmpty() ? null : lensColor;
        }

        public void setLensColor(String value) {
            lensColor = value == null ? "" : value;
            markClientDirty();
        }

        public int mode() {
            return mode;
        }

        public String foundSign() {
            return foundSign;
        }

        public ItemStack input() {
            return inventory.getStackInSlot(0);
        }

        public ItemStack glass() {
            return inventory.getStackInSlot(1);
        }

        /** 绘制进度（§6.4.2 的 `Run` 字段）。 */
        public int runTick() {
            return runTick;
        }

        public int parchment() {
            return parchment;
        }

        public ItemStackHandler inventory() {
            return inventory;
        }

        public LazyOptional<IItemHandler> itemHandler() {
            return handler;
        }

        /** §6.6.1：星图台的投料分派（羊皮纸批量投放 / 玻璃入槽 / 书与可附魔物入输入槽）。 */
        public InteractionResult interactStarChart(
                Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held) {
            if (held.is(lensItem("parchment"))) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                int space = 64 - parchment;
                if (space <= 0) return InteractionResult.SUCCESS;
                int moved = Math.min(space, held.getCount());
                parchment += moved;
                held.shrink(moved);
                play(level, pos, "book_flip");
                markClientDirty();
                return InteractionResult.SUCCESS;
            }
            if (held.isEmpty()) {
                if (player.isShiftKeyDown()) {
                    if (level.isClientSide) return InteractionResult.SUCCESS;
                    dropSlot(level, pos, 0);
                    dropSlot(level, pos, 1);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
            if (held.getItem() instanceof BlockItem) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            int target = glass().isEmpty() && isGlass(held) ? 1 : 0;
            if (inventory.getStackInSlot(target).isEmpty()) {
                inventory.setStackInSlot(target, held.split(1));
                markClientDirty();
            }
            return InteractionResult.SUCCESS;
        }

        private static boolean isGlass(ItemStack stack) {
            var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id != null && id.getPath().equals("star_glass");
        }

        private static Item lensItem(String path) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Omphalos.MODID, path));
            return item == null ? Items.AIR : item;
        }

        private void dropSlot(Level level, BlockPos pos, int slot) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) return;
            Block.popResource(level, pos, stack);
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }

        public void dropContents(Level level, BlockPos pos) {
            dropSlot(level, pos, 0);
            dropSlot(level, pos, 1);
            while (parchment > 0) {
                int chunk = Math.min(64, parchment);
                parchment -= chunk;
                var item = lensItem("parchment");
                if (item != Items.AIR) Block.popResource(level, pos, new ItemStack(item, chunk));
            }
        }

        private static void play(Level level, BlockPos pos, String soundId) {
            var sound =
                    ForgeRegistries.SOUND_EVENTS.getValue(
                            new ResourceLocation(Omphalos.MODID, soundId));
            if (sound != null) level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.7F, 1.0F);
        }

        /** 服务端 tick：星图台 200 tick 绘制、共鸣坛待机、林木信标充能。 */
        public void serverTick() {
            if (level == null || level.isClientSide) return;
            var block = getBlockState().getBlock();
            if (!(block instanceof MachineBlock machine)) return;
            switch (machine.kind()) {
                case STAR_CHART_TABLE -> {
                    if (runTick > 0) {
                        runTick = Math.max(0, runTick - 1);
                        if (runTick == 0) markClientDirty();
                    }
                }
                case GROVE_BEACON -> {
                    if (charge < 1000) {
                        charge = Math.min(1000, charge + 1);
                        if (((int) charge) % 100 == 0) markClientDirty();
                    }
                }
                case RESONANCE_ALTAR -> {
                    boolean standing =
                            !level.getEntitiesOfClass(
                                            Player.class,
                                            new net.minecraft.world.phys.AABB(worldPosition).inflate(2.5))
                                    .isEmpty();
                    int next = standing ? 1 : 0;
                    if (next != mode) {
                        mode = next;
                        markClientDirty();
                    }
                }
                default -> {}
            }
        }

        public boolean drawChart(ServerPlayer player, java.util.List<com.mpp.stellaeomphalos.network.toServer.PktImprintEngrave.Stroke> strokes) {
            if (player instanceof net.minecraftforge.common.util.FakePlayer || level==null || level.isClientSide
                    || !getBlockState().is(STAR_CHART_TABLE.get()) || player.level()!=level
                    || player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)>64
                    || runTick>0 || parchment<1 || !glass().is(com.mpp.stellaeomphalos.content.item.PartSixItems.STAR_GLASS.get())
                    || !com.mpp.stellaeomphalos.content.item.StarGlassItem.chart(glass()).isEmpty() || strokes.size()!=3
                    || !player.mayBuild() || !level.mayInteract(player, worldPosition)) return false;
            var progress=com.mpp.stellaeomphalos.player.progress.StarRecords.get(player);
            if(!progress.valid())return false;
            var signs=new java.util.ArrayList<ResourceLocation>();
            var positions=new net.minecraft.nbt.ListTag();
            for(var stroke:strokes) {
                var sign=com.mpp.stellaeomphalos.constellation.sign.SignRegistry.byNumericId(stroke.signId());
                if(sign==null||!progress.knownSigns().contains(sign.id())||signs.contains(sign.id())
                        ||stroke.gridX()<0||stroke.gridX()>30||stroke.gridZ()<0||stroke.gridZ()>30)return false;
                signs.add(sign.id());var point=new CompoundTag();point.putString("Sign",sign.id().toString());
                point.putInt("X",stroke.gridX());point.putInt("Z",stroke.gridZ());positions.add(point);
            }
            parchment--;
            boolean burned=false;
            for(int count=1;count<=3;count++)for(int attempt=0;attempt<count;attempt++)if(level.random.nextInt(100)<2)burned=true;
            if(!burned){com.mpp.stellaeomphalos.content.item.StarGlassItem.engrave(glass(),signs,level.random.nextLong());glass().getOrCreateTag().getCompound("Chart").put("Placements",positions);}
            runTick=burned?0:200;markClientDirty();
            player.displayClientMessage(Component.translatable(burned?"stellaeomphalos.visual.chart_burned":"stellaeomphalos.visual.chart_complete"),true);
            play(level,worldPosition,burned?"ritual_fail":"codex_page_turn");return true;
        }

        /** 星图台开始一次绘制（上限 200 tick）。 */
        public void beginDrawing() {
            runTick = 200;
            setChanged();
        }

        @Override
        protected void writePersistent(CompoundTag tag) {
            tag.put("Items", inventory.serializeNBT());
            tag.putInt("Rotation", rotation);
            tag.putDouble("Charge", charge);
            tag.putInt("Run", runTick);
            tag.putInt("Paper", parchment);
            tag.putString("LensColor", lensColor);
            tag.putInt("Mode", mode);
            tag.putString("Sign", foundSign);
            if (owner != null) tag.putString("Owner", owner.toString());
        }

        @Override
        protected void readPersistent(CompoundTag tag) {
            if (tag.contains("Items", Tag.TAG_COMPOUND)) inventory.deserializeNBT(tag.getCompound("Items"));
            rotation = tag.getInt("Rotation");
            charge = tag.getDouble("Charge");
            runTick = tag.getInt("Run");
            parchment = tag.getInt("Paper");
            lensColor = tag.getString("LensColor");
            mode = tag.getInt("Mode");
            foundSign = tag.getString("Sign");
            owner = tag.contains("Owner") ? parseUuid(tag.getString("Owner")) : null;
        }

        private static UUID parseUuid(String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        @Override
        protected void writeClientState(CompoundTag tag) {
            tag.putInt("Rotation", rotation);
            tag.putInt("Run", runTick);
            tag.putInt("Paper", parchment);
            tag.putInt("Mode", mode);
            tag.putString("LensColor", lensColor);
            tag.putString("Sign", foundSign);
            tag.putBoolean("HasOwner", owner != null);
        }

        @Override
        protected void readClientState(CompoundTag tag) {
            rotation = tag.getInt("Rotation");
            runTick = tag.getInt("Run");
            parchment = tag.getInt("Paper");
            mode = tag.getInt("Mode");
            lensColor = tag.getString("LensColor");
            foundSign = tag.getString("Sign");
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            handler.invalidate();
        }

        @Override
        public <T> LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> capability,
                Direction side) {
            if (capability == ForgeCapabilities.ITEM_HANDLER) return handler.cast();
            return super.getCapability(capability, side);
        }
    }
}
