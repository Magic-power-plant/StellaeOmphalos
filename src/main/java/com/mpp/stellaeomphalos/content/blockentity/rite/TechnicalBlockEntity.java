package com.mpp.stellaeomphalos.content.blockentity.rite;

import com.mpp.stellaeomphalos.content.block.TechnicalBlock;
import com.mpp.stellaeomphalos.content.world.WorldContent;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared technical host lifecycle; links are bounded, loaded-only and never recursive. */
public final class TechnicalBlockEntity extends BlockEntity
        implements com.mpp.stellaeomphalos.content.item.PartSixRodItems.WandInteractable,
                com.mpp.stellaeomphalos.content.item.PartSixRodItems.LinkHandler {
    private BlockPos host;
    private BlockPos link;
    private BlockState mimic = Blocks.STONE.defaultBlockState();
    private long ticks;
    private int boreY, headWear;
    private int vortexLayer;
    private int seatId = -1;
    private boolean playerPlaced;
    private String wandColor = "white";
    private int boostTimeout;
    private java.util.UUID seatUuid;
    private String installedHead = "";
    private boolean active;

    private final net.minecraft.world.phys.AABB visualBounds;
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox() { return visualBounds; }
    public TechnicalBlockEntity(BlockPos p, BlockState s) {
        super(WorldContent.TECH_ENTITY.get(), p, s);
        visualBounds = new net.minecraft.world.phys.AABB(p.getX()-2,p.getY()-1,p.getZ()-2,p.getX()+3,p.getY()+4,p.getZ()+3);
        boreY = p.getY() - 2;
    }

    @Override public void onLoad() {
        super.onLoad();
        if(level instanceof ServerLevel server && getBlockState().is(WorldContent.BLOCKS.get("gate_core").get()))
            com.mpp.stellaeomphalos.content.world.GateLedger.get(server).add(worldPosition);
    }

    public void host(BlockPos position) {
        if (position.equals(worldPosition) || position.distSqr(worldPosition) > 4096)
            throw new IllegalArgumentException("Invalid host link");
        host = position.immutable();
        setChanged();
    }

    public BlockPos host() {
        return host;
    }

    public void mimic(BlockState s) {
        if (s.getBlock() instanceof TechnicalBlock)
            throw new IllegalArgumentException("Recursive mimic");
        mimic = s;
        setChanged();
    }

    public BlockState hostState() {
        if (level != null && host != null && level.hasChunkAt(host)) {
            var s = level.getBlockState(host);
            if (!(s.getBlock() instanceof TechnicalBlock)) return s;
        }
        return mimic;
    }

    public InteractionResult interact(Player player, InteractionHand hand, BlockHitResult hit) {
        var kind = ((TechnicalBlock) getBlockState().getBlock()).kind();
        if (!player.mayBuild() || !level.mayInteract(player, worldPosition)) return InteractionResult.FAIL;
        if ((kind.equals("frame_shell") || kind.equals("mirage_shell"))
                && player.mayBuild() && level.mayInteract(player, worldPosition)) {
            var held = player.getItemInHand(hand);
            if (held.getItem() instanceof net.minecraft.world.item.BlockItem item
                    && !(item.getBlock() instanceof TechnicalBlock)) {
                if (!level.isClientSide) { mimic(item.getBlock().defaultBlockState()); syncAppearance(); }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (kind.equals("frame_shell") && player.isShiftKeyDown() && held.isEmpty()) {
                var target = worldPosition.relative(hit.getDirection().getOpposite());
                if (level.hasChunkAt(target) && level.mayInteract(player, target)
                        && !level.getBlockState(target).isAir()
                        && !(level.getBlockState(target).getBlock() instanceof TechnicalBlock)) {
                    if (!level.isClientSide) { host(target); syncAppearance(); }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        if (kind.equals("frame_shell") && host != null && level.hasChunkAt(host)
                && level.mayInteract(player, host)) {
            var target = level.getBlockState(host);
            if (!(target.getBlock() instanceof TechnicalBlock))
                return target.use(
                        level,
                        player,
                        hand,
                        new BlockHitResult(
                                hit.getLocation(), hit.getDirection(), host, hit.isInside()));
        }
        if (kind.equals("observatory") && player.isShiftKeyDown()
                && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem())
                        .toString().equals("stellaeomphalos:sign_chart"))
            return InteractionResult.sidedSuccess(level.isClientSide);
        if (kind.equals("rite_link")) {
            // §6.6.1：右键把本端作为链接起点/终点（符文杖与接线器共用同一套语义）。
            onLinkUse(player, player.getItemInHand(hand));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind.equals("gate_core")) {
            if(player instanceof net.minecraft.server.level.ServerPlayer server)
                com.mpp.stellaeomphalos.content.world.GateNetworkService.open(server,worldPosition);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind.equals("observatory")) {
            // §6.2.9 / §6.6.1：右键骑乘观星座并打开观星台容器，用于判定玩家是否停在自由观察位。
            if (!level.isClientSide
                    && player instanceof net.minecraft.server.level.ServerPlayer server) {
                var seat = findSeat();
                if (seat == null && !player.isPassenger() && level instanceof ServerLevel seatLevel)
                    seat = spawnSeat(seatLevel);
                if (seat != null && !player.isPassenger()) player.startRiding(seat, true);
                net.minecraftforge.network.NetworkHooks.openScreen(
                        server,
                        new net.minecraft.world.SimpleMenuProvider(
                                (id, inventory, ignored) ->
                                        new com.mpp.stellaeomphalos.content.menu.PartSixMenus
                                                .ObservatoryMenu(id, inventory, worldPosition),
                                net.minecraft.network.chat.Component.translatable(
                                        "container.stellaeomphalos.observatory")),
                        buffer ->
                                com.mpp.stellaeomphalos.content.menu.PartSixMenus.writePos(
                                        buffer, worldPosition));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            active = !active;
            setChanged();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void syncAppearance() {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ---------------------------------------------------------------- 星仪链接（§6.6.1）

    /** 链接端点选择键：同时用于符文杖与共鸣接线器，两条入口语义一致。 */
    public static final String LINK_PENDING = "LinkPending";

    /** §6.4.2：本链接器已配对的对端坐标；未链接时为 null。 */
    public BlockPos linkedTo() {
        return link;
    }

    /**
     * 尝试把本端与 {@code other} 双向配对（§6.6.1：`tryLink` 要求对端尚未链接）。
     *
     * @return 是否成功建立链接
     */
    public boolean tryLink(BlockPos other) {
        if (other == null || other.equals(worldPosition) || !(level instanceof ServerLevel server))
            return false;
        if (link != null) return false;
        if (!server.hasChunkAt(other)) return false;
        if (!(server.getBlockEntity(other) instanceof TechnicalBlockEntity peer)) return false;
        if (peer.link != null) return false;
        link = other.immutable();
        peer.link = worldPosition.immutable();
        setChanged();
        peer.setChanged();
        return true;
    }

    /** 拆除一端时清掉对端的链接，避免留下悬空引用。 */
    private void clearPeerLink() {
        if (link == null || !(level instanceof ServerLevel server)) return;
        if (server.hasChunkAt(link)
                && server.getBlockEntity(link) instanceof TechnicalBlockEntity peer
                && worldPosition.equals(peer.link)) {
            peer.link = null;
            peer.setChanged();
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) clearPeerLink();
        super.setRemoved();
    }

    /**
     * 链接点击入口（符文杖的 `WandInteractable` 与接线器的 `LinkHandler` 共用）。
     *
     * <p>第一次点击在物品 NBT 的 {@link #LINK_PENDING} 上记录起点，第二次点击尝试双向配对；
     * 成功或失败都会给出可见反馈，避免"点了没反应"。
     */
    public boolean onLinkUse(
            net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack tool) {
        if (level == null || level.isClientSide) return true;
        if (!(getBlockState().getBlock() instanceof TechnicalBlock technical)
                || !technical.kind().equals("rite_link")) return false;
        var tag = tool.getOrCreateTag();
        if (!tag.contains(LINK_PENDING)) {
            tag.putLong(LINK_PENDING, worldPosition.asLong());
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "item.stellaeomphalos.resonance_linker.first"),
                    true);
            return true;
        }
        var first = BlockPos.of(tag.getLong(LINK_PENDING));
        boolean linked = false;
        if (level instanceof ServerLevel server
                && server.hasChunkAt(first)
                && server.getBlockEntity(first) instanceof TechnicalBlockEntity origin
                && origin
                        .getBlockState()
                        .getBlock()
                        .equals(
                                com.mpp.stellaeomphalos.content.world.WorldContent.BLOCKS
                                        .get("rite_link")
                                        .get()))
            linked = origin.tryLink(worldPosition);
        tag.remove(LINK_PENDING);
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        linked
                                ? "item.stellaeomphalos.resonance_linker.linked"
                                : "item.stellaeomphalos.resonance_linker.failed"),
                true);
        return true;
    }

    @Override
    public boolean onWandUse(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.item.ItemStack wand,
            net.minecraft.core.Direction side) {
        return onLinkUse(player, wand);
    }

    @Override
    public boolean onLink(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.item.ItemStack linker) {
        return onLinkUse(player, linker);
    }

    /** §6.4.2：解析观星辅助实体；id 失效时返回 null。 */
    private com.mpp.stellaeomphalos.content.entity.p6.ObservatorySeatEntity findSeat() {
        if (!(level instanceof ServerLevel server) || seatId < 0) return null;
        return server.getEntity(seatId)
                        instanceof
                        com.mpp.stellaeomphalos.content.entity.p6.ObservatorySeatEntity seat
                ? seat
                : null;
    }

    private static java.util.UUID parseUuid(String value) {
        try {
            return java.util.UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** §6.4.2：是否由真实玩家放置（照明器只有玩家放置才工作）。 */
    public boolean playerPlaced() {
        return playerPlaced;
    }

    /** 记录"由玩家放置"。 */
    public void markPlayerPlaced() {
        if (playerPlaced) return;
        playerPlaced = true;
        setChanged();
    }

    /** §6.6.2：照明杖把颜色交给照明器并清空临时星能。 */
    public void handOverColor(net.minecraft.world.item.DyeColor color, int boostTicks) {
        wandColor = color == null ? "white" : color.getName();
        boostTimeout = Math.max(0, boostTicks);
        setChanged();
    }

    /** §6.4.2：当前光色名。 */
    public String wandColorName() {
        return wandColor;
    }

    /** §6.4.2：照明杖加速剩余 tick。 */
    public int boostTimeout() {
        return boostTimeout;
    }

    public void setBoostTimeout(int ticks) {
        boostTimeout = Math.max(0, ticks);
        setChanged();
    }

    /** §6.4.2：维持观星辅助实体；缺失时在方块位置重建。 */
    private com.mpp.stellaeomphalos.content.entity.p6.ObservatorySeatEntity spawnSeat(
            ServerLevel server) {
        var seat =
                new com.mpp.stellaeomphalos.content.entity.p6.ObservatorySeatEntity(
                        com.mpp.stellaeomphalos.content.entity.p6.PartSixEntities.OBSERVATORY_SEAT
                                .get(),
                        server);
        seat.setFixed(worldPosition);
        seat.moveTo(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.0D,
                worldPosition.getZ() + 0.5D,
                0.0F,
                0.0F);
        if (!server.addFreshEntity(seat)) return null;
        seatId = seat.getId();
        seatUuid = seat.getUUID();
        setChanged();
        return seat;
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        ticks++;
        if (!active || ticks % 20 != 0) return;
        String kind = ((TechnicalBlock) getBlockState().getBlock()).kind();
        if (kind.equals("fountain")) {
            var below = worldPosition.below();
            var headState = server.getBlockState(below);
            // §6.2.1.2 / D-4：泉头是单一方块的两个模式变体。
            if (!(headState.getBlock()
                    instanceof com.mpp.stellaeomphalos.content.block.BoreHeadBlock)) return;
            var mode = headState.getValue(com.mpp.stellaeomphalos.content.block.BoreHeadBlock.MODE);
            var tier = headState.getValue(com.mpp.stellaeomphalos.content.block.BoreHeadBlock.TIER);
            if (mode == com.mpp.stellaeomphalos.content.block.BoreHeadBlock.BoreMode.VORTEX) {
                tickVortex(server, below, tier);
                return;
            }
            tickLiquid(server, below, tier);
        }
        if (kind.equals("luminaire") && boostTimeout > 0 && ticks % 20 == 0) {
            // §6.4.2：照明杖加速计时；归零后回到常规投放节奏。
            setBoostTimeout(boostTimeout - 20);
        }
        if (kind.equals("observatory")) {
            // §6.4.2：每 tick 维持辅助实体；被清掉就重建。
            if (findSeat() == null) spawnSeat(server);
        }
        if (kind.equals("ore_regenerator")
                && server.getBlockState(worldPosition.above()).isAir()
                && server.random.nextInt(20) == 0) {
            var selected =
                    com.mpp.stellaeomphalos.content.world.WorldBehaviorRegistry.randomOre(
                            server.random, "mineral");
            if (selected != null) server.setBlock(worldPosition.above(), selected, 3);
        }
    }

    /** `LIQUID` 模式：向下开挖倒锥井，并按档位决定工具与磨损（§6.6.1）。 */
    private void tickLiquid(
            ServerLevel server, BlockPos headPos, com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier tier) {
        var target = new BlockPos(worldPosition.getX(), boreY, worldPosition.getZ());
        if (server.isOutsideBuildHeight(target)) {
            active = false;
            return;
        }
        if (!server.hasChunkAt(target)) return;
        var s = server.getBlockState(target);
        var tool = com.mpp.stellaeomphalos.content.block.BoreHeadBlock.toolFor(tier);
        var headKey = "bore_head_" + tier.getSerializedName();
        if (!installedHead.equals(headKey)) {
            installedHead = headKey;
            headWear = 0;
        }
        if (s.getDestroySpeed(server, target) < 0
                || s.requiresCorrectToolForDrops()
                        && !new net.minecraft.world.item.ItemStack(tool).isCorrectToolForDrops(s)) {
            active = false;
            setChanged();
            return;
        }
        if (!s.isAir()
                && server.destroyBlock(target, true)
                && ++headWear >= tool.getMaxDamage()) {
            server.destroyBlock(headPos, false);
            headWear = 0;
            installedHead = "";
            active = false;
        }
        boreY--;
        setChanged();
    }

    /** `VORTEX` 模式：清空 7×7×7 并把非玩家生物拉入冻结（§6.6.1）。 */
    private void tickVortex(
            ServerLevel server, BlockPos headPos, com.mpp.stellaeomphalos.content.block.BoreHeadBlock.Tier tier) {
        var center = new BlockPos(worldPosition.getX(), boreY, worldPosition.getZ());
        if (server.isOutsideBuildHeight(center)) {
            active = false;
            return;
        }
        if (!server.hasChunkAt(center)) return;
        // 每 tick 只处理一层，保持有界工作量。
        int layer = Math.floorMod(vortexLayer, 7) - 3;
        vortexLayer++;
        var tool = com.mpp.stellaeomphalos.content.block.BoreHeadBlock.toolFor(tier);
        for (int x = -3; x <= 3; x++)
            for (int z = -3; z <= 3; z++) {
                var target = center.offset(x, layer, z);
                if (!server.hasChunkAt(target) || server.isOutsideBuildHeight(target)) continue;
                var state = server.getBlockState(target);
                if (state.isAir() || state.getDestroySpeed(server, target) < 0) continue;
                if (state.requiresCorrectToolForDrops()
                        && !new net.minecraft.world.item.ItemStack(tool).isCorrectToolForDrops(state))
                    continue;
                server.destroyBlock(target, true);
            }
        // 把漩涡内的非玩家生物拉向中心并施加时间冻结。
        for (var entity :
                server.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        new net.minecraft.world.phys.AABB(center).inflate(4.0D))) {
            if (entity instanceof net.minecraft.world.entity.player.Player) continue;
            var pull = net.minecraft.world.phys.Vec3.atCenterOf(center).subtract(entity.position());
            if (pull.lengthSqr() > 1.0E-4D)
                entity.setDeltaMovement(entity.getDeltaMovement().add(pull.normalize().scale(0.12D)));
            entity.addEffect(
                    new net.minecraft.world.effect.MobEffectInstance(
                            com.mpp.stellaeomphalos.content.effect.PartSixEffects.TIME_FREEZE.get(),
                            80,
                            0,
                            false,
                            false));
        }
        setChanged();
    }

    protected void saveAdditional(CompoundTag n) {
        super.saveAdditional(n);
        if (host != null) n.putLong("Host", host.asLong());
        n.put("Mimic", NbtUtils.writeBlockState(mimic));
        n.putLong("Ticks", ticks);
        n.putInt("BoreY", boreY);
        n.putInt("HeadWear", headWear);
        n.putInt("VortexLayer", vortexLayer);
        n.putBoolean("Placed", playerPlaced);
        n.putString("Color", wandColor);
        n.putInt("Boost", boostTimeout);
        if (link != null) n.putLong("Link", link.asLong());
        n.putInt("SeatId", seatId);
        if (seatUuid != null) n.putString("SeatUuid", seatUuid.toString());
        n.putString("InstalledHead", installedHead);
        n.putBoolean("Active", active);
    }

    public void load(CompoundTag n) {
        super.load(n);
        host = n.contains("Host") ? BlockPos.of(n.getLong("Host")) : null;
        mimic = n.contains("Mimic", Tag.TAG_COMPOUND) ?
                NbtUtils.readBlockState(
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),
                        n.getCompound("Mimic")) : Blocks.STONE.defaultBlockState();
        if (mimic.getBlock() instanceof TechnicalBlock) mimic = Blocks.STONE.defaultBlockState();
        ticks = n.getLong("Ticks");
        boreY = n.contains("BoreY") ? n.getInt("BoreY") : worldPosition.getY() - 2;
        active = n.getBoolean("Active");
        headWear = Math.max(0, n.getInt("HeadWear"));
        vortexLayer = Math.max(0, n.getInt("VortexLayer"));
        playerPlaced = n.getBoolean("Placed");
        wandColor = n.contains("Color") ? n.getString("Color") : "white";
        boostTimeout = Math.max(0, n.getInt("Boost"));
        link = n.contains("Link") ? BlockPos.of(n.getLong("Link")) : null;
        seatId = n.contains("SeatId") ? n.getInt("SeatId") : -1;
        seatUuid = n.contains("SeatUuid") ? parseUuid(n.getString("SeatUuid")) : null;
        installedHead = n.getString("InstalledHead");
    }
}
