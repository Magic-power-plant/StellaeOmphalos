package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.Omphalos;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 《方块物品实体完整清单》§6.2.2.4 / §6.2.2.5 的杖类与功能物品实现。
 *
 * <p>符文杖族共用 {@link BlockPalette} 方块收藏夹；照明杖按 §6.6.2 放置/移除光斑并把普通方块包成
 * `mirage_shell`；抓钩杖发射 {@code grapnel} 实体；辉光粉/暗影粉分别投射 {@code lucent_spark} 与
 * `umbral_spark`。客户端专属的手部/HUD 渲染与观星镜界面归《客户端渲染界面与音效》。
 */
public final class RodItems {

    private RodItems() {}

    private static Block block(String id) {
        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Omphalos.MODID, id));
        if (block == null) throw new IllegalStateException("Missing block " + id);
        return block;
    }

    /** 由方块接口定义的法杖交互点（等价原模组的 `IWandInteract`）。 */
    public interface WandInteractable {
        /** @return 是否消费本次交互 */
        boolean onWandUse(Level level, BlockPos pos, Player player, ItemStack wand, Direction side);
    }

    /** 由方块接口定义的链接点（等价原模组的 `ILinkableTile`）。 */
    public interface LinkHandler {
        /** @return 是否消费本次链接点击 */
        boolean onLink(Level level, BlockPos pos, Player player, ItemStack linker);
    }

    /** 符文杖增幅（§6.6.2）。按**名字**持久化，不使用 ordinal。 */
    public enum WandAugment implements net.minecraft.util.StringRepresentable {
        /** 无增幅。 */
        NONE,
        /** 铺路：选中时把脚下 3×3 的可替换格转为 `phase_barrier`。 */
        AEVITAS,
        /** 跃迁：蓄力后松开发射，`motion = look × clamp(tick/30,0,1) × 3`。 */
        VICIO,
        /** 御盾：使用时呈盾牌姿态（`blocking` 语义）。 */
        ARMARA,
        /** 寻矿：每 20 tick 收集附近的晶簇矿候选。 */
        PROSPECT;

        /** NBT 键。 */
        public static final String TAG = "Augment";

        /** 默认增幅。 */
        public static final WandAugment DEFAULT = NONE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public String translationKey() {
            return "item.stellaeomphalos.runed_wand.augment." + getSerializedName();
        }

        /** 由根名解析；未知或缺失时回退 {@link #DEFAULT}。 */
        public static WandAugment parse(String raw) {
            if (raw == null) return DEFAULT;
            for (var candidate : values())
                if (candidate.name().equalsIgnoreCase(raw)) return candidate;
            return DEFAULT;
        }

        /** 下一个增幅（潜行右键轮换顺序）。 */
        public WandAugment next() {
            var values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    /** 读取符文杖当前的增幅。 */
    public static WandAugment augmentOf(ItemStack stack) {
        if (!stack.hasTag()) return WandAugment.DEFAULT;
        return WandAugment.parse(stack.getTag().getString(WandAugment.TAG));
    }

    /** 写入增幅。 */
    public static void setAugment(ItemStack stack, WandAugment augment) {
        stack.getOrCreateTag().putString(WandAugment.TAG, augment.getSerializedName());
    }

    /**
     * 符文杖（§6.2.2.4 / §6.6.2）。
     *
     * <p>右键方块：对方块与方块实体**双重询问**，实现必须幂等。
     * 潜行右键：轮换增幅。非潜行使用：按增幅分派（`VICIO` 蓄力跃迁、`ARMARA` 举盾、
     * `AEVITAS` 铺路、`PROSPECT` 寻矿）。
     */
    public static final class RunedWandItem extends Item {

        /** `VICIO` 满蓄力所需 tick。 */
        public static final int CHARGE_TICKS = 30;

        /** `VICIO` 满蓄力时的速度倍率。 */
        public static final float CHARGE_SCALE = 3.0F;

        /** `VICIO` 的 Y 分量保底比例。 */
        public static final double CHARGE_Y_FLOOR = 0.7D;

        /** `AEVITAS` 铺路的间隔（tick）。 */
        public static final int PAVE_INTERVAL = 10;

        /** 上次铺路的游戏刻键。 */
        public static final String LAST_PAVE = "LastPave";

        /** 上次寻矿的游戏刻键。 */
        public static final String LAST_PROSPECT = "LastProspect";

        /** `PROSPECT` 扫描的间隔（tick）。 */
        public static final int PROSPECT_INTERVAL = 20;

        /** `PROSPECT` 扫描半径（格）。 */
        public static final int PROSPECT_RADIUS = 32;

        public RunedWandItem() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            var state = level.getBlockState(pos);
            boolean handled = false;
            if (state.getBlock() instanceof WandInteractable blockWand)
                handled =
                        blockWand.onWandUse(
                                level, pos, player, context.getItemInHand(), context.getClickedFace());
            if (level.getBlockEntity(pos) instanceof WandInteractable entityWand)
                handled |=
                        entityWand.onWandUse(
                                level, pos, player, context.getItemInHand(), context.getClickedFace());
            return handled ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    var next = augmentOf(stack).next();
                    setAugment(stack, next);
                    level.playSound(null,player.blockPosition(),com.mpp.stellaeomphalos.content.particle.ClientVisualContent.sound("wand_augment_switch"),SoundSource.PLAYERS,.5F,1);
                    player.displayClientMessage(
                            Component.translatable(
                                    "item.stellaeomphalos.runed_wand.augment",
                                    Component.translatable(next.translationKey())),
                            true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            // 需要蓄力的增幅进入"使用中"状态，由 releaseUsing 结算。
            if (augmentOf(stack) == WandAugment.VICIO)
                return InteractionResultHolder.consume(stack);
            if (augmentOf(stack) == WandAugment.PROSPECT && !level.isClientSide)
                prospect(player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return 72000;
        }

        @Override
        public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
            return augmentOf(stack) == WandAugment.ARMARA
                    ? net.minecraft.world.item.UseAnim.BLOCK
                    : net.minecraft.world.item.UseAnim.NONE;
        }

        @Override
        public void releaseUsing(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity, int timeLeft) {
            if (level.isClientSide || augmentOf(stack) != WandAugment.VICIO) return;
            if (!(entity instanceof Player player)) return;
            int used = getUseDuration(stack) - timeLeft;
            float mul = Math.max(0.0F, Math.min(1.0F, used / (float) CHARGE_TICKS)) * CHARGE_SCALE;
            if (mul <= 0.0F) return;
            var look = player.getLookAngle();
            double y = Math.max(look.y * mul, CHARGE_Y_FLOOR * mul);
            player.setDeltaMovement(look.x * mul, y, look.z * mul);
            player.fallDistance = 0.0F;
            player.hasImpulse = true;
            player.hurtMarked = true;
        }

        @Override
        public void inventoryTick(
                ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
            if (level.isClientSide || !selected || !(entity instanceof Player player)) return;
            var augment = augmentOf(stack);
            // 节流按**杖自身**记录上次执行刻，避免依赖全局对齐的游戏刻。
            if (augment == WandAugment.AEVITAS) pave(level, player, stack);
            else if (augment == WandAugment.PROSPECT) prospect(player, stack);
        }

        /** `AEVITAS`：把脚下 3×3 的可替换格转成相位屏障（有界，仅 9 格）。 */
        private static void pave(Level level, Player player, ItemStack stack) {
            long now = level.getGameTime();
            var tag = stack.getOrCreateTag();
            if (now - tag.getLong(LAST_PAVE) < PAVE_INTERVAL) return;
            tag.putLong(LAST_PAVE, now);
            var barrier =
                    com.mpp.stellaeomphalos.content.world.WorldContent.BLOCKS.get("phase_barrier");
            if (barrier == null) return;
            var feet = player.blockPosition().below();
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++) {
                    var target = feet.offset(x, 0, z);
                    if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) continue;
                    var state = level.getBlockState(target);
                    if (!state.isAir() && !state.canBeReplaced()) continue;
                    level.setBlock(target, barrier.get().defaultBlockState(), Block.UPDATE_ALL);
                }
        }

        /** `PROSPECT`：有界扫描附近的晶簇矿并写入候选缓存；投递归《服务端机制数据存储与网络协议》。 */
        private static void prospect(Player player, ItemStack stack) {
            if (!(player.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
            long now = server.getGameTime();
            var tag = stack.getOrCreateTag();
            if (now - tag.getLong(LAST_PROSPECT) < PROSPECT_INTERVAL) return;
            tag.putLong(LAST_PROSPECT, now);
            var found = new java.util.ArrayList<BlockPos>(16);
            var center = player.blockPosition();
            int budget = 0;
            for (var pos :
                    BlockPos.betweenClosed(
                            center.offset(-PROSPECT_RADIUS, -24, -PROSPECT_RADIUS),
                            center.offset(PROSPECT_RADIUS, 24, PROSPECT_RADIUS))) {
                if (budget++ > WandProspectCache.MAX_INSPECTED) break;
                if (!server.hasChunkAt(pos)) continue;
                var state = server.getBlockState(pos);
                if (state.is(com.mpp.stellaeomphalos.content.world.WorldContent.GEODE_ORE.get())
                        && !com.mpp.stellaeomphalos.content.block.GeodeOreBlock.isAstral(state))
                    found.add(pos.immutable());
                if (found.size() >= WandProspectCache.MAX_ENTRIES) break;
            }
            WandProspectCache.publish(player.getUUID(), found);
        }
    }

    /** `PROSPECT` 增幅的候选缓存：有界、按玩家索引，供《服务端机制数据存储与网络协议》的结果包消费。 */
    public static final class WandProspectCache {

        /** 单次扫描的最大检查格数。 */
        public static final int MAX_INSPECTED = 8192;

        /** 单个玩家保留的最大候选数。 */
        public static final int MAX_ENTRIES = 256;

        private static final java.util.Map<java.util.UUID, java.util.List<BlockPos>> RESULTS =
                new java.util.concurrent.ConcurrentHashMap<>();

        private WandProspectCache() {}

        /** 发布一次扫描结果（截断到上限）。 */
        public static void publish(java.util.UUID player, java.util.List<BlockPos> found) {
            if (player == null) return;
            RESULTS.put(
                    player,
                    found.size() <= MAX_ENTRIES
                            ? java.util.List.copyOf(found)
                            : java.util.List.copyOf(found.subList(0, MAX_ENTRIES)));
        }

        /** @return 该玩家最近一次扫描结果 */
        public static java.util.List<BlockPos> last(java.util.UUID player) {
            return RESULTS.getOrDefault(player, java.util.List.of());
        }

        /** 玩家退网时清理。 */
        public static void clear(java.util.UUID player) {
            RESULTS.remove(player);
        }

        /** 停服清理。 */
        public static void clearAll() {
            RESULTS.clear();
        }
    }

    /**
     * 一次放置计划（§6.6.2）：目标位置 + 要写入的状态 + 要扣除的物品。
     *
     * <p>"先模拟扣料再实扣"由 {@link #plan} + {@link #commit} 两段完成：
     * 规划阶段在一份**虚拟库存**上扣减，料不够就提前停止（不会出现"扣了料但没放下"）；
     * 提交阶段才真正 `shrink` 玩家背包并写方块。
     */
    public record Placement(BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.item.Item item) {}

    /** 逐格可放性判定；返回 false 即停止扩展（剪力保护）。 */
    @FunctionalInterface
    public interface PlacementRule {
        boolean canPlace(net.minecraft.world.level.Level level, BlockPos pos);
    }

    /** 默认规则：只能替换可替换方块（构筑杖）。 */
    public static final PlacementRule REPLACEABLE_ONLY =
            (level, pos) -> level.getBlockState(pos).canBeReplaced();

    /**
     * 规划一次铺设：沿候选序列逐格判定，并在虚拟库存上模拟扣料。
     *
     * @param candidates 候选位置（顺序即铺设顺序）
     * @param rule 每格的可放性判定；返回 false 立即停止（"剪力不可替换实心方块之后的位置"）
     * @param limit 本次最多放置的格数
     */
    public static List<Placement> plan(
            net.minecraft.world.level.Level level,
            Player player,
            List<BlockPos> candidates,
            List<net.minecraft.world.level.block.state.BlockState> states,
            PlacementRule rule,
            int limit) {
        if (states.isEmpty()) return List.of();
        var inventory = player.getInventory();
        var available = new java.util.HashMap<net.minecraft.world.item.Item, Integer>();
        if (!player.getAbilities().instabuild) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                var stack = inventory.getItem(i);
                if (!stack.isEmpty()) available.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        var plan = new ArrayList<Placement>();
        int index = 0;
        for (var pos : candidates) {
            if (plan.size() >= limit) break;
            if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) continue;
            if (!rule.canPlace(level, pos)) break;
            var state = states.get(index % states.size());
            var item = state.getBlock().asItem();
            if (item == net.minecraft.world.item.Items.AIR) break;
            if (!player.getAbilities().instabuild) {
                int have = available.getOrDefault(item, 0);
                if (have <= 0) break; // 料不够：停止规划，避免半成品
                available.put(item, have - 1);
            }
            plan.add(new Placement(pos.immutable(), state, item));
            index++;
        }
        return List.copyOf(plan);
    }

    /** 提交计划：实扣物品并写方块；返回实际放置格数。 */
    public static int commit(net.minecraft.world.level.Level level, Player player, List<Placement> plan) {
        boolean creative = player.getAbilities().instabuild;
        int placed = 0;
        for (var placement : plan) {
            if (creative || consumeOne(player, placement.item()))
                if (level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL)) placed++;
        }
        return placed;
    }

    /** 从背包扣掉一件指定物品；找不到时返回 false（调用方据此跳过该格）。 */
    private static boolean consumeOne(Player player, net.minecraft.world.item.Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) continue;
            stack.shrink(1);
            if (stack.isEmpty()) inventory.setItem(i, ItemStack.EMPTY);
            inventory.setChanged();
            return true;
        }
        return false;
    }

    /** 构筑杖：潜行右键存入方块状态，右键沿命中面铺开（§6.6.2）。 */
    public static class BuilderRodItem extends Item {

        /** 一次放置的最大格数。 */
        public static final int MAX_PLACEMENT = 20;

        public BuilderRodItem() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            var stack = context.getItemInHand();
            if (player.isShiftKeyDown()) {
                if (!level.isClientSide
                        && BlockPalette.tryStore(stack, level, context.getClickedPos())) {
                    level.playSound(
                            null,
                            context.getClickedPos(),
                            SoundEvents.ITEM_FRAME_ADD_ITEM,
                            SoundSource.PLAYERS,
                            0.6F,
                            1.2F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (BlockPalette.isEmpty(stack)) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            var states = BlockPalette.storedStates(stack);
            if (states.isEmpty()) return InteractionResult.PASS;
            var origin = context.getClickedPos().relative(context.getClickedFace());
            var spread = context.getClickedFace().getAxis().isVertical()
                    ? context.getHorizontalDirection()
                    : Direction.UP;
            var candidates = new ArrayList<BlockPos>(MAX_PLACEMENT);
            for (int step = 0; step < MAX_PLACEMENT; step++) {
                var target = origin.relative(spread, step / 2);
                if (step % 2 == 1) target = target.relative(spread.getClockWise(), 1);
                candidates.add(target);
            }
            var plan = plan(level, player, candidates, states, REPLACEABLE_ONLY, MAX_PLACEMENT);
            int placed = commit(level, player, plan);
            if (placed > 0) {
                BlockPalette.clear(player);
                level.playSound(null, origin, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        /** 工具提示行：显示当前库存数量。 */
        @Override
        public void appendHoverText(
                ItemStack stack, Level level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
            tooltip.add(
                    Component.translatable(
                            "item.stellaeomphalos.block_rod.stored", BlockPalette.storedStates(stack).size()));
        }
    }

    /** 置换杖：把同质区域内的方块替换为库存状态（§6.6.2）。 */
    public static final class SwapperRodItem extends BuilderRodItem {

        /** 搜索深度（格）。 */
        public static final int SEARCH_DEPTH = 5;

        /** 可替换方块的硬度门槛默认值（配置键 `gameplay.blockRodHardnessLimit`）。 */
        public static final double DEFAULT_HARDNESS_LIMIT = 3.0D;

        /** 从配置读取硬度门槛；不可用时退回默认值。 */
        public static double hardnessLimit() {
            var value =
                    com.mpp.stellaeomphalos.OmphalosConfig.SERVER
                            .snapshot()
                            .get("gameplay.blockRodHardnessLimit");
            return value instanceof Number number ? number.doubleValue() : DEFAULT_HARDNESS_LIMIT;
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var player = context.getPlayer();
            if (player == null || player.isShiftKeyDown()) return super.useOn(context);
            if (BlockPalette.isEmpty(context.getItemInHand())) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            var states = BlockPalette.storedStates(context.getItemInHand());
            var origin = context.getClickedPos();
            var match = level.getBlockState(origin);
            double limit = hardnessLimit();
            var candidates = new ArrayList<BlockPos>(MAX_PLACEMENT);
            for (var pos : BlockPos.betweenClosed(
                    origin.offset(-SEARCH_DEPTH, -SEARCH_DEPTH, -SEARCH_DEPTH),
                    origin.offset(SEARCH_DEPTH, SEARCH_DEPTH, SEARCH_DEPTH))) {
                if (candidates.size() >= MAX_PLACEMENT) break;
                var current = level.getBlockState(pos);
                if (!current.is(match.getBlock())) continue;
                // 基岩类（硬度 < 0）与超过门槛的方块一律放弃。
                float hardness = current.getDestroySpeed(level, pos);
                if (hardness < 0 || hardness > limit) continue;
                candidates.add(pos.immutable());
            }
            // 置换的目标本身是实心方块，故不使用"可替换"规则；逐格扣料仍然成立。
            var plan = plan(level, player, candidates, states, (lvl, pos) -> true, MAX_PLACEMENT);
            int replaced = commit(level, player, plan);
            if (replaced > 0) {
                BlockPalette.clear(player);
                level.playSound(null, origin, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.7F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** 照明杖：着色放置光斑、把普通方块包成幻影外壳、给照明器传色（§6.6.2）。 */
    public static final class LuminaryRodItem extends Item {

        /** 交给照明器的加速时长（tick）。 */
        public static final int LUMINAIRE_BOOST_TICKS = 6000;

        /** 照明杖自身的颜色键。 */
        public static final String COLOR = "Color";

        public LuminaryRodItem() {
            super(new Item.Properties().stacksTo(1));
        }

        /** 当前颜色；未设置时为白色。 */
        public static net.minecraft.world.item.DyeColor currentColor(ItemStack stack) {
            if (!stack.hasTag()) return net.minecraft.world.item.DyeColor.WHITE;
            var raw = stack.getTag().getString(COLOR);
            for (var color : net.minecraft.world.item.DyeColor.values())
                if (color.getName().equalsIgnoreCase(raw)) return color;
            return net.minecraft.world.item.DyeColor.WHITE;
        }

        /** 轮换颜色（潜行右键空气时使用）。 */
        public static net.minecraft.world.item.DyeColor nextColor(ItemStack stack) {
            var values = net.minecraft.world.item.DyeColor.values();
            var next = values[(currentColor(stack).getId() + 1) % values.length];
            stack.getOrCreateTag().putString(COLOR, next.getName());
            return next;
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && !level.isClientSide) {
                var color = nextColor(stack);
                player.displayClientMessage(
                        Component.translatable(
                                "item.stellaeomphalos.luminary_rod.color", color.getName()),
                        true);
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var pos = context.getClickedPos();
            var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            var stack = context.getItemInHand();
            var mote = block("glow_mote");
            var mirage = block("mirage_shell");
            // §6.6.2：右键照明器 → 把当前颜色交给它并清空全部临时星能。
            var luminaire = com.mpp.stellaeomphalos.content.world.WorldContent.BLOCKS.get("luminaire");
            if (luminaire != null && level.getBlockState(pos).is(luminaire.get())) {
                if (!level.isClientSide
                        && level.getBlockEntity(pos)
                                instanceof
                                com.mpp.stellaeomphalos.content.blockentity.rite
                                        .TechnicalBlockEntity be) {
                    be.handOverColor(currentColor(stack), LUMINAIRE_BOOST_TICKS);
                    // 临时星能由《星辉能量与星象星眷系统》的充能层持有；此处只清理杖自身的本次投放缓存。
                    BlockPalette.clear(player);
                    level.playSound(
                            null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.2F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            var above = pos.relative(context.getClickedFace());
            if (player.isShiftKeyDown()) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                // 潜行：把普通方块包成幻影外壳；再次潜行右键还原。
                var current = level.getBlockState(above);
                if (current.is(mirage)) {
                    if (level.getBlockEntity(above)
                            instanceof com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity be) {
                        var host = be.hostState();
                        if (!host.isAir()) {
                            level.setBlock(above, host, Block.UPDATE_ALL);
                            return InteractionResult.SUCCESS;
                        }
                    }
                    return InteractionResult.PASS;
                }
                if (current.isAir() || current.canBeReplaced()) {
                    if (level.setBlock(above, mirage.defaultBlockState(), Block.UPDATE_ALL)
                            && level.getBlockEntity(above)
                                    instanceof
                                    com.mpp.stellaeomphalos.content.blockentity.rite
                                            .TechnicalBlockEntity be) {
                        be.mimic(current.isAir() ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState() : current);
                        level.sendBlockUpdated(above, mirage.defaultBlockState(), mirage.defaultBlockState(), 3);
                        return InteractionResult.SUCCESS;
                    }
                }
                return InteractionResult.PASS;
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;
            // 非潜行：放置/移除光斑（同一状态再次右键则移除）。
            var target = level.getBlockState(above);
            if (target.is(mote)) {
                level.removeBlock(above, false);
            } else if (target.isAir() || target.canBeReplaced()) {
                var colored = mote.defaultBlockState();
                if (colored.hasProperty(com.mpp.stellaeomphalos.content.block.LightMoteBlock.COLOR))
                    colored = colored.setValue(com.mpp.stellaeomphalos.content.block.LightMoteBlock.COLOR, currentColor(stack));
                level.setBlock(above, colored, Block.UPDATE_ALL);
            } else {
                return InteractionResult.PASS;
            }
            level.playSound(null, above, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.6F);
            return InteractionResult.SUCCESS;
        }
    }

    /** 抓钩杖：发射 `grapnel` 实体（§6.2.2.4）。 */
    public static final class GrapnelRodItem extends Item {

        /** 冷却（tick）。 */
        public static final int COOLDOWN = 40;

        public GrapnelRodItem() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                var entity =
                        new com.mpp.stellaeomphalos.content.entity.catalog.GrapnelEntity(
                                com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities.GRAPNEL.get(),
                                player,
                                level);
                entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 1.0F);
                level.addFreshEntity(entity);
                player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN);
                level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    /** 共鸣接线器：把右键转发给链接处理器（§6.6.2）。 */
    public static final class ResonanceLinkerItem extends Item {
        public ResonanceLinkerItem() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            var pos = context.getClickedPos();
            if (level.getBlockEntity(pos) instanceof LinkHandler handler
                    && handler.onLink(level, pos, player, context.getItemInHand()))
                return InteractionResult.sidedSuccess(level.isClientSide);
            if (level.getBlockState(pos).getBlock() instanceof LinkHandler handler
                    && handler.onLink(level, pos, player, context.getItemInHand()))
                return InteractionResult.sidedSuccess(level.isClientSide);
            return InteractionResult.PASS;
        }
    }

    /** 手持观星镜：服务端不处理，客户端打开专用界面（界面归《客户端渲染界面与音效》）。 */
    public static final class HandSpyglassItem extends ClientRenderedItem {
        public HandSpyglassItem() {
            super(new Item.Properties().stacksTo(1));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer server)
                com.mpp.stellaeomphalos.network.SafeDispatch.send(server,
                        new com.mpp.stellaeomphalos.network.toClient.PktOpenObservation("hand_telescope", java.util.Optional.empty()));
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
    }

    /** 玫瑰木弓：纯表现层遗留物品，不进创造页（§6.2.2.5 标记为简化）。 */
    public static final class RosewoodBowItem extends ProjectileWeaponItem {
        public RosewoodBowItem() {
            super(new Item.Properties().stacksTo(1).durability(384));
        }

        @Override
        public java.util.function.Predicate<ItemStack> getAllSupportedProjectiles() {
            return stack -> stack.is(net.minecraft.world.item.Items.ARROW);
        }

        @Override
        public int getDefaultProjectileRange() {
            return 15;
        }
    }

    /** 辉光粉：就地放置易逝光源，否则投射辉光火花（§6.6.2）。 */
    public static final class IlluminationDustItem extends Item {
        public IlluminationDustItem() {
            super(new Item.Properties().stacksTo(64));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            var target = context.getClickedPos().relative(context.getClickedFace());
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (com.mpp.stellaeomphalos.content.entity.catalog.LucentSparkEntity.canBeLit(level, target)
                    && com.mpp.stellaeomphalos.content.entity.catalog.LucentSparkEntity.placeLightAt(level, target)) {
                level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5F, 1.4F);
                consume(context.getItemInHand(), player);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                var spark =
                        new com.mpp.stellaeomphalos.content.entity.catalog.LucentSparkEntity(
                                com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities.LUCENT_SPARK.get(),
                                player,
                                level);
                spark.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.7F, 0.9F);
                level.addFreshEntity(spark);
                consume(stack, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    /** 暗影粉：偏移一格投射幽影火花并进入刷新态（§6.6.2）。 */
    public static final class NocturnalDustItem extends Item {
        public NocturnalDustItem() {
            super(new Item.Properties().stacksTo(64));
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            var level = context.getLevel();
            var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            var target = context.getClickedPos().relative(context.getClickedFace());
            var spark =
                    new com.mpp.stellaeomphalos.content.entity.catalog.UmbralSparkEntity(
                            com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities.UMBRAL_SPARK.get(),
                            player,
                            level);
            spark.setSpawning(true);
            spark.setPos(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
            level.addFreshEntity(spark);
            consume(context.getItemInHand(), player);
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                var spark =
                        new com.mpp.stellaeomphalos.content.entity.catalog.UmbralSparkEntity(
                                com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities.UMBRAL_SPARK.get(),
                                player,
                                level);
                spark.setSpawning(true);
                spark.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.7F, 0.9F);
                level.addFreshEntity(spark);
                consume(stack, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    /** 星盘/共鸣器等共用的一次性消耗。 */
    static void consume(ItemStack stack, Player player) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    /** 供测试与提示使用：由侧向与命中点计算目标格。 */
    public static BlockPos offsetTarget(BlockHitResult hit, boolean offset) {
        return offset ? hit.getBlockPos().relative(hit.getDirection()) : hit.getBlockPos();
    }

    /** 服务端校验携带者是否处于可交互距离。 */
    static boolean inReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= 64.0D;
    }
}
