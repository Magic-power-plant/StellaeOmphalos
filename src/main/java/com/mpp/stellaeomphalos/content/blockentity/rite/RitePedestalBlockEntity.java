package com.mpp.stellaeomphalos.content.blockentity.rite;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.constellation.sign.*;
import com.mpp.stellaeomphalos.content.blockentity.lumen.LumenSinkBlockEntity;
import com.mpp.stellaeomphalos.content.world.WorldContent;
import com.mpp.stellaeomphalos.ritual.amplifier.AmplifierTier;
import com.mpp.stellaeomphalos.ritual.effect.RiteScheduler;
import com.mpp.stellaeomphalos.ritual.rite.*;
import com.mpp.stellaeomphalos.structure.match.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Owner custody, energy and inventory survive reload; runtime watches and effects are rebuilt. */
public final class RitePedestalBlockEntity extends LumenSinkBlockEntity
        implements RiteHost, RitePedestal, StructureAnchor, StructureDependent {
    private UUID owner;
    private ItemStack crystal = ItemStack.EMPTY, output = ItemStack.EMPTY;
    private RiteInstance rite;
    private StructureState structure = StructureState.INDETERMINATE;
    private OutputHoldMode hold = OutputHoldMode.HELD;
    private int offlineTicks;
    private RiteState clientRiteState = RiteState.IDLE;
    private int clientProgress;
    public RiteState displayedState() { return level != null && level.isClientSide ? clientRiteState : rite == null ? RiteState.IDLE : rite.state(); }
    public int displayedProgress() { return level != null && level.isClientSide ? clientProgress : rite == null ? 0 : rite.progress(); }
    private long age;
    private final List<Amplifier> amplifiers = new ArrayList<>();

    private record Amplifier(
            BlockPos position, String slot, ResourceLocation id, AmplifierTier tier) {}

    public RitePedestalBlockEntity(BlockPos pos, BlockState state) {
        super(WorldContent.PEDESTAL_ENTITY.get(), pos, state, 64000);
    }

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID value) {
        if (owner != null && !owner.equals(value))
            throw new IllegalStateException("Owner is immutable");
        owner = Objects.requireNonNull(value);
        setChanged();
    }

    public OutputHoldMode holdMode() {
        return hold;
    }

    public RiteInstance rite() {
        return rite;
    }

    public ItemStack crystal() {
        return crystal.copy();
    }

    public ItemStack output() {
        return output.copy();
    }

    public ResourceLocation blueprintId() {
        return new ResourceLocation("stellaeomphalos", "pattern_rite_pedestal");
    }

    public StructureState structureState() {
        return structure;
    }

    public void onStructureStateChanged(StructureState next, StructureState previous) {
        structure = next;
        if (!next.canProduce() && level instanceof ServerLevel server)
            RiteScheduler.suspend(server, worldPosition);
        markClientDirty();
    }

    public void stateChanged(RiteState old, RiteState next) {
        if (level instanceof ServerLevel server
                && (next == RiteState.WARMUP || next == RiteState.FINISHING)) {
            var sound =
                    net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(
                            new ResourceLocation(
                                    "stellaeomphalos",
                                    next == RiteState.WARMUP ? "ritual_start" : "rite_output"));
            if (sound != null)
                server.playSound(
                        null,
                        worldPosition,
                        sound,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        0.7F,
                        1);
        }
        markClientDirty();
        broadcastState();
        if (next != RiteState.RUNNING && level instanceof ServerLevel server)
            RiteScheduler.suspend(server, worldPosition);
    }

    private void broadcastState() {
        if (!(level instanceof ServerLevel server) || rite == null) return;
        var recipe = RiteRegistry.recipes().get(rite.recipeId());
        int percent =
                recipe == null
                        ? 0
                        : Math.min(
                                100, (int) (100L * rite.progress() / rite.effectiveCycle(recipe)));
        var packet =
                new com.mpp.stellaeomphalos.network.toClient.RiteStatePayload(
                        worldPosition,
                        rite.state().ordinal(),
                        percent,
                        recipe == null ? 0 : (int) (recipe.baseIntensity() * 1000),
                        structure.ordinal());
        for (var player : server.players())
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))
                    <= 1024) com.mpp.stellaeomphalos.network.OmphalosChannel.send(player, packet);
    }

    public boolean authorized(ServerPlayer player) {
        if (owner == null) return player.hasPermissions(2);
        if (owner.equals(player.getUUID()) || player.hasPermissions(2)) return true;
        if (!OmphalosConfig.SERVER.flag("ritual.allowTeamCollect")) return false;
        var owning = player.server.getPlayerList().getPlayer(owner);
        return owning != null && owning.isAlliedTo(player);
    }

    public boolean tryDeliver(ServerPlayer player) {
        if (!authorized(player)
                || output.isEmpty()
                || player.level() != level
                || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))
                        > 64) return false;
        var before = output.getCount();
        player.getInventory().add(output);
        if (output.isEmpty()) output = ItemStack.EMPTY;
        setChanged();
        return output.getCount() < before;
    }

    public boolean interact(ServerPlayer player, InteractionHand hand, int action) {
        if (!authorized(player)
                || player.level() != level
                || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))
                        > 36) return false;
        if (action == 2) {
            hold = OutputHoldMode.values()[(hold.ordinal() + 1) % OutputHoldMode.values().length];
            markClientDirty();
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "stellaeomphalos.rite.hold_mode", net.minecraft.network.chat.Component.translatable(
                            "stellaeomphalos.rite.hold." + hold.name().toLowerCase(java.util.Locale.ROOT))), true);
            return true;
        }
        if (action == 1) {
            if (tryDeliver(player)) return true;
            if (!crystal.isEmpty() && player.getItemInHand(hand).isEmpty()) {
                player.setItemInHand(hand, crystal);
                crystal = ItemStack.EMPTY;
                setChanged();
                return true;
            }
            if (rite != null) rite.reset(this);
            return true;
        }
        if (action != 0) return false;
        var held = player.getItemInHand(hand);
        if (crystal.isEmpty()
                && !held.isEmpty()
                && held.getTag() != null
                && held.getTag().contains("CrystalTraits")) {
            crystal = held.split(1);
            rite = null;
            markClientDirty();
            return true;
        }
        if (rite != null) rite.requestStart();
        return true;
    }

    public void environmentChanged() {
        if (!(level instanceof ServerLevel server)) return;
        onNeighborChanged(level, worldPosition);
        if (!crystal.isEmpty()
                && server.hasChunkAt(worldPosition.above())
                && server.getBlockState(worldPosition.above()).isSolid()) {
            drop(crystal);
            crystal = ItemStack.EMPTY;
            setChanged();
        }
        if (rite != null) rite.requestStart();
    }

    public boolean crystalValid(RiteRecipe recipe) {
        if (crystal.isEmpty() || crystal.getTag() == null) return false;
        var traits = crystal.getTag().getCompound("CrystalTraits");
        return traits.getInt("Size") >= recipe.minSize()
                && traits.getInt("Purity") >= recipe.minPurity()
                && (recipe.tunedTo() == null
                        || recipe.tunedTo()
                                .toString()
                                .equals(crystal.getTag().getString("SignId")));
    }

    public boolean celestialReady(RiteRecipe recipe) {
        return !recipe.requiresSignRisen()
                || SignSkyService.activeSigns(level).stream()
                        .anyMatch(s -> s.id().equals(recipe.sign()));
    }

    public long storedLumen() {
        return lumenStored();
    }

    public void consumeLumen(int amount) {
        if (consumeLumen(amount, false) != amount)
            throw new IllegalStateException("Rite charge without reservation");
    }

    private void scanAmplifiers() {
        amplifiers.clear();
        if (!(level instanceof ServerLevel server)) return;
        for (int x = -4; x <= 4; x++)
            for (int z = -4; z <= 4; z++) {
                int ring = Math.max(Math.abs(x), Math.abs(z));
                if (ring != 2 && ring != 4) continue;
                var p = worldPosition.offset(x, 0, z);
                if (!server.hasChunkAt(p)) continue;
                var id =
                        net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(
                                server.getBlockState(p).getBlock());
                if (id == null || !id.getPath().startsWith("rite_amplifier_")) continue;
                var tierId = new ResourceLocation(id.getNamespace(), id.getPath().substring(15));
                var tier = RiteRegistry.amplifiers().get(tierId);
                if (tier != null)
                    amplifiers.add(new Amplifier(p, ring == 2 ? "inner" : "outer", tierId, tier));
            }
    }

    public boolean amplifiersReady(RiteRecipe recipe) {
        for (var entry : recipe.amplifierSlots().entrySet()) {
            var req = entry.getValue();
            var relevant =
                    amplifiers.stream()
                            .filter(
                                    a ->
                                            a.slot().equals(entry.getKey())
                                                    && a.tier().compatible(recipe.sign())
                                                    && (req.tiers().isEmpty()
                                                            || req.tiers().contains(a.id())))
                            .toList();
            if (relevant.size() < req.min() || relevant.size() > req.max()) return false;
            for (var amp : relevant)
                if (relevant.stream().filter(a -> a.id().equals(amp.id())).count()
                        > amp.tier().maxPerSlot()) return false;
        }
        return true;
    }

    public int amplifierUpkeep() {
        return amplifiers.stream().mapToInt(a -> a.tier().lumenUpkeepPerTick()).sum();
    }

    public boolean commitOutputs(List<ItemStack> values) {
        ItemStack candidate = output.copy();
        for (var stack : values) {
            if (candidate.isEmpty()) candidate = stack.copy();
            else if (!ItemStack.isSameItemSameTags(candidate, stack)
                    || candidate.getCount() + stack.getCount() > candidate.getMaxStackSize())
                return false;
            else candidate.grow(stack.getCount());
        }
        output = candidate;
        setChanged();
        return true;
    }

    public void cycleCompleted() {
        if (level instanceof ServerLevel server && owner != null) {
            var player = server.getServer().getPlayerList().getPlayer(owner);
            if (player != null)
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new com.mpp.stellaeomphalos.core.platform.ProgressMilestoneEvent(
                                player, "rite", rite.recipeId(), rite.cycles()));
        }
        if (level instanceof ServerLevel server && owner != null && !output.isEmpty()) {
            var player = server.getServer().getPlayerList().getPlayer(owner);
            if (player != null)
                com.mpp.stellaeomphalos.network.OmphalosChannel.send(
                        player,
                        new com.mpp.stellaeomphalos.network.toClient.RiteOutputPayload(
                                worldPosition, output, rite.cycles()));
        }
        for (var a : amplifiers)
            if (a.tier().consumesOnCycle() && level.hasChunkAt(a.position())) {
                var original = level.getBlockState(a.position());
                var expected = WorldContent.BLOCKS.get("rite_amplifier_" + a.id().getPath());
                if (expected != null && original.is(expected.get()))
                    level.destroyBlock(a.position(), false);
            }
        setChanged();
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;
        age++;
        if (rite != null && rite.state() == RiteState.RUNNING && age % 80 == 0) {
            var sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("stellaeomphalos", "ritual_loop"));
            if (sound != null) server.playSound(null, worldPosition, sound, net.minecraft.sounds.SoundSource.BLOCKS, .35F, 1);
        }
        structure = StructureIntegrityHub.of(server).query(worldPosition, blueprintId());
        if (age % 20 == 1) scanAmplifiers();
        if (rite == null && !crystal.isEmpty())
            RiteRegistry.recipes().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(e -> crystalValid(e.getValue()))
                    .findFirst()
                    .ifPresent(e -> rite = new RiteInstance(e.getKey()));
        if (rite != null) {
            var recipe = RiteRegistry.recipes().get(rite.recipeId());
            rite.tick(recipe, this, server.random);
            if (recipe != null && rite.state() == RiteState.RUNNING) {
                double factor =
                        amplifiers.stream()
                                .mapToDouble(a -> a.tier().factor(recipe.sign()))
                                .reduce(1, (a, b) -> a * b);
                float intensity =
                        AmplifierTier.intensity(
                                recipe.baseIntensity(),
                                factor,
                                structure == StructureState.DEGRADED ? 1 : 0,
                                rite.interrupts());
                RiteScheduler.schedule(
                        server,
                        worldPosition,
                        owner,
                        amplifiers.size(),
                        intensity,
                        recipe,
                        () ->
                                !isRemoved()
                                        && rite.state() == RiteState.RUNNING
                                        && structureState().canProduce());
            }
        }
        var player = owner == null ? null : server.getServer().getPlayerList().getPlayer(owner);
        offlineTicks = player == null ? Math.min(Integer.MAX_VALUE - 1, offlineTicks) + 1 : 0;
        if (hold == OutputHoldMode.AUTO_INVENTORY && player != null) tryDeliver(player);
        if (hold == OutputHoldMode.DROP_ON_FULL
                && !output.isEmpty()
                && offlineTicks > OmphalosConfig.SERVER.integer("ritual.offlineDropThreshold")
                && output.getCount() >= output.getMaxStackSize()) {
            drop(output);
            output = ItemStack.EMPTY;
        }
        setChanged();
        if (age % 40 == 0) {
            markClientDirty();
            broadcastState();
        }
    }

    private void drop(ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return;
        var entity =
                new ItemEntity(
                        server,
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 1,
                        worldPosition.getZ() + 0.5,
                        stack.copy());
        if (owner != null) entity.setTarget(owner);
        entity.setDefaultPickUpDelay();
        server.addFreshEntity(entity);
    }

    public void dropOwnedContents() {
        if (level instanceof ServerLevel server) RiteScheduler.detach(server, worldPosition);
        drop(crystal);
        drop(output);
        crystal = output = ItemStack.EMPTY;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server)
            StructureIntegrityHub.of(server)
                    .observe(
                            worldPosition,
                            blueprintId(),
                            com.mpp.stellaeomphalos.structure.pattern.PlacementTransform.NONE);
    }

    @Override
    public void onChunkUnloaded() {
        if (level instanceof ServerLevel server) RiteScheduler.detach(server, worldPosition);
        super.onChunkUnloaded();
    }

    protected void writePersistent(CompoundTag n) {
        super.writePersistent(n);
        if (owner != null) n.putUUID("Owner", owner);
        n.put("Crystal", crystal.save(new CompoundTag()));
        n.put("Output", output.save(new CompoundTag()));
        if (rite != null) n.put("RiteInstance", rite.save());
        n.putString("HoldMode", hold.name());
        n.putInt("OfflineTicks", offlineTicks);
        n.putLong("Age", age);
    }

    protected void readPersistent(CompoundTag n) {
        super.readPersistent(n);
        owner = n.hasUUID("Owner") ? n.getUUID("Owner") : null;
        crystal = ItemStack.of(n.getCompound("Crystal"));
        output = ItemStack.of(n.getCompound("Output"));
        rite = n.contains("RiteInstance") ? RiteInstance.load(n.getCompound("RiteInstance")) : null;
        try {
            hold = OutputHoldMode.valueOf(n.getString("HoldMode"));
        } catch (IllegalArgumentException e) {
            hold = OutputHoldMode.HELD;
        }
        offlineTicks = Math.max(0, n.getInt("OfflineTicks"));
        age = Math.max(0, n.getLong("Age"));
    }

    protected void writeClientState(CompoundTag n) {
        super.writeClientState(n);
        n.putString("Structure", structure.name());
        n.putString("HoldMode", hold.name());
        n.putString("RiteState", rite == null ? RiteState.IDLE.name() : rite.state().name());
        n.putInt("Progress", rite == null ? 0 : rite.progress());
    }

    protected void readClientState(CompoundTag n) {
        super.readClientState(n);
        try { clientRiteState = RiteState.valueOf(n.getString("RiteState")); }
        catch (IllegalArgumentException invalid) { clientRiteState = RiteState.IDLE; }
        clientProgress = Math.max(0, n.getInt("Progress"));
        try { hold = OutputHoldMode.valueOf(n.getString("HoldMode")); }
        catch (IllegalArgumentException invalid) { hold = OutputHoldMode.HELD; }
        try {
            structure = StructureState.valueOf(n.getString("Structure"));
        } catch (IllegalArgumentException ex) {
            structure = StructureState.INDETERMINATE;
        }
    }
}
