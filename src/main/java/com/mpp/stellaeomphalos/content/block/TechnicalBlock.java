package com.mpp.stellaeomphalos.content.block;

import com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

import java.util.List;
import java.util.Set;

/**
 * 《方块物品实体完整清单》§6.2.1.4 技术方块族：不可获得、无掉落、特殊渲染。
 *
 * <p>`frame_shell` 把音效/硬度/抗性/拾取/碰撞/拆除粒子转发给下方宿主；`phase_barrier` 保留满格碰撞并屏蔽一切视觉；
 * `proxy_foliage` / `mirage_shell` 转发被伪装的方块状态；`rupture_anchor` 每 60 tick 校验结构并自毁。
 * 具体语义由 {@code kind} 分派，方块实体统一由 {@link TechnicalBlockEntity} 承担。
 */
public final class TechnicalBlock extends Block implements EntityBlock {

    /** 转发宿主外观的 kinds：声音/硬度/拾取/碰撞全部转给宿主。 */
    private static final Set<String> FORWARDING =
            Set.of("frame_shell", "mirage_shell", "proxy_foliage");

    /** 完全不可见的 kinds。 */
    private static final Set<String> INVISIBLE =
            Set.of("frame_shell", "phase_barrier", "proxy_foliage", "rupture_anchor", "gate_node");

    private final String kind;

    public TechnicalBlock(String kind) {
        super(propertiesFor(kind));
        this.kind = kind;
    }

    /** §6.3.4 的渲染类型 / 碰撞 / 发光 / 可替换 / 无掉落登记表。 */
    private static Properties propertiesFor(String kind) {
        var properties =
                Properties.of().noOcclusion().pushReaction(PushReaction.BLOCK);
        return switch (kind) {
            case "frame_shell", "phase_barrier", "proxy_foliage" ->
                    properties.strength(-1.0F, 3600000.0F).noLootTable();
            case "rupture_anchor" ->
                    properties.strength(-1.0F, 3600000.0F).lightLevel(s -> 3).noLootTable().randomTicks();
            case "mirage_shell" -> properties.strength(0.3F).noLootTable();
            default -> properties.strength(3.0F, 3600000.0F);
        };
    }

    public String kind() {
        return kind;
    }

    @Override
    public BlockState getStateForPlacement(
            net.minecraft.world.item.context.BlockPlaceContext context) {
        // 泉头的放置守卫已移交 `BoreHeadBlock`（§6.2.1.2 / D-4），技术方块族本身无额外约束。
        return defaultBlockState();
    }

    @Override
    public SoundType getSoundType(
            BlockState state, LevelReader level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (FORWARDING.contains(kind)
                && level.getBlockEntity(pos) instanceof TechnicalBlockEntity be)
            return be.hostState().getSoundType(level, pos, entity);
        return super.getSoundType(state, level, pos, entity);
    }

    @Override
    public float getDestroyProgress(
            BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (kind.equals("frame_shell")
                && world.getBlockEntity(pos) instanceof TechnicalBlockEntity be)
            return be.hostState().getDestroyProgress(player, world, pos);
        return super.getDestroyProgress(state, player, world, pos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return Set.of(
                                "frame_shell",
                                "mirage_shell",
                                "proxy_foliage",
                                "gate_core",
                                "luminaire",
                                "rite_link",
                                "observatory",
                                "fountain",
                                "ore_regenerator")
                        .contains(kind)
                ? new TechnicalBlockEntity(p, s)
                : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return INVISIBLE.contains(kind) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState s, BlockGetter level, BlockPos p, CollisionContext ctx) {
        if (kind.equals("phase_barrier") || kind.equals("rupture_anchor")) return Shapes.block();
        if (kind.equals("gate_node") || kind.equals("glow_mote")) return Shapes.empty();
        if (FORWARDING.contains(kind) && level.getBlockEntity(p) instanceof TechnicalBlockEntity be)
            return be.hostState().getCollisionShape(level, p, ctx);
        return super.getCollisionShape(s, level, p, ctx);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos p, CollisionContext ctx) {
        if (kind.equals("phase_barrier") || kind.equals("glow_mote")) return Shapes.empty();
        if (kind.equals("rupture_anchor")) return Shapes.block();
        if (FORWARDING.contains(kind) && level.getBlockEntity(p) instanceof TechnicalBlockEntity be)
            return be.hostState().getShape(level, p, ctx);
        return super.getShape(s, level, p, ctx);
    }

    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        if (kind.equals("frame_shell")) {
            var origin = params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
            var level = params.getLevel();
            if (origin != null
                    && level.getBlockEntity(BlockPos.containing(origin))
                            instanceof TechnicalBlockEntity be) {
                var host = be.hostState();
                if (!host.isAir()) return List.of(new ItemStack(host.getBlock()));
            }
            return List.of();
        }
        return super.getDrops(state, params);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos p, BlockState s) {
        if (FORWARDING.contains(kind)
                && level.getBlockEntity(p) instanceof TechnicalBlockEntity be)
            return new ItemStack(be.hostState().getBlock());
        return super.getCloneItemStack(level, p, s);
    }

    @Override
    public InteractionResult use(
            BlockState s,
            Level level,
            BlockPos p,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.getBlockEntity(p) instanceof TechnicalBlockEntity be)
            return be.interact(player, hand, hit);
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            net.minecraft.world.entity.LivingEntity placer,
            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (kind.equals("gate_core") && level instanceof ServerLevel server)
            com.mpp.stellaeomphalos.content.world.GateLedger.get(server).add(pos);
        // §6.4.2：照明器记录"玩家放置"（忽略伪玩家）。
        if (level.isClientSide || !kind.equals("luminaire")) return;
        if (placer instanceof net.minecraft.world.entity.player.Player player
                && !(player instanceof net.minecraftforge.common.util.FakePlayer)
                && level.getBlockEntity(pos)
                        instanceof
                        com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity be)
            be.markPlayerPlaced();
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos from,
            boolean moving) {
        if (level.isClientSide) return;
        if (kind.equals("frame_shell")
                && level.getBlockEntity(pos) instanceof TechnicalBlockEntity be
                && be.host() == null
                && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
            level.removeBlock(pos, false);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // §6.6.1：遗迹锚点每 60 tick 校验一次所需结构，不成立则自毁。
        if (kind.equals("rupture_anchor")
                && random.nextInt(3) == 0
                && !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP))
            level.removeBlock(pos, false);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level l, BlockState s, BlockEntityType<T> type) {
        return l.isClientSide
                ? null
                : (world, p, state, be) -> {
                    if (be instanceof TechnicalBlockEntity technical) technical.serverTick();
                };
    }
}
