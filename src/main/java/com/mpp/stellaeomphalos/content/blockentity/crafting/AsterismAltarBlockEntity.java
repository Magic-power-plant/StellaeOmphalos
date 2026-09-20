package com.mpp.stellaeomphalos.content.blockentity.crafting;

import com.mpp.stellaeomphalos.constellation.sign.SignSkyService;
import com.mpp.stellaeomphalos.core.util.math.SkyDensityField;
import com.mpp.stellaeomphalos.crafting.altar.menu.SignFocusProvider;
import com.mpp.stellaeomphalos.crafting.altar.recipe.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** Five tiers share one block and one entity type; upgrading changes only the tier property. */
public final class AsterismAltarBlockEntity extends AbstractCraftingMachine {
    private long carry;
    private SkyDensityField density;
    private boolean chain;

    public AsterismAltarBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingContent.ALTAR_ENTITY.get(), pos, state, 25);
    }

    @Override
    public String machineKind() {
        return "asterism";
    }

    @Override
    public long lumenCapacity() {
        return tier().lumenCapacity();
    }

    @Override
    public long acceptLumen(net.minecraft.world.level.Level level, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        long accepted =
                Math.min(
                        amount,
                        (lumenCapacity() - lumenStored()) / AsterismEnergy.NETWORK_MULTIPLIER);
        if (accepted > 0)
            super.acceptLumen(level, accepted * AsterismEnergy.NETWORK_MULTIPLIER, simulate);
        return accepted;
    }

    public AsterismRecipeInput input(UUID crafter) {
        var input = stacks();
        input.add(focus.getStackInSlot(0));
        var focused = focus.getStackInSlot(0);
        var sign =
                focused.getItem() instanceof SignFocusProvider provider
                        ? provider.sign(focused).orElse(null)
                        : null;
        return new AsterismRecipeInput(
                tier(), lumenStored(), sign, id -> unlocked(crafter, id), input);
    }

    private List<CraftingRelayBlockEntity> relays(ServerLevel server) {
        var result = new ArrayList<CraftingRelayBlockEntity>();
        for (int x = -5; x <= 5; x++)
            for (int z = -5; z <= 5; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) < 4) continue;
                var pos = worldPosition.offset(x, 0, z);
                if (server.hasChunkAt(pos)
                        && server.getBlockEntity(pos) instanceof CraftingRelayBlockEntity relay)
                    result.add(relay);
            }
        return result;
    }

    private boolean relayRequirements(
            ServerLevel server, AsterismRecipe recipe, AsterismCraftTask craft, boolean bind) {
        var available = relays(server);
        var used =
                Collections.newSetFromMap(new IdentityHashMap<CraftingRelayBlockEntity, Boolean>());
        for (var requirement : recipe.relays()) {
            var previous = craft.bindings().get(requirement.index());
            var candidate =
                    available.stream()
                            .filter(
                                    r ->
                                            !used.contains(r)
                                                    && requirement
                                                            .material()
                                                            .test(r.inventory().getStackInSlot(0))
                                                    && (previous == null
                                                            || previous.equals(r.position())))
                            .findFirst();
            if (candidate.isEmpty()) {
                craft.unbind(requirement.index());
                return false;
            }
            var relay = candidate.get();
            used.add(relay);
            if (bind && craft.progressFraction() >= requirement.bindAt())
                craft.bind(requirement.index(), relay.position());
        }
        // Unconfigured occupied relay slots are forbidden within the altar's crafting ring.
        return available.stream()
                .allMatch(r -> used.contains(r) || r.inventory().getStackInSlot(0).isEmpty());
    }

    @Override
    protected void onCollected(Player player) {
        if (chain && task == null) start(player);
    }

    @Override
    protected boolean start(Player player) {
        if (!(level instanceof ServerLevel server)) return false;
        var hub = CraftingBootstrap.hub(server.getServer());
        var candidates = new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>();
        candidates.addAll(hub.byInput(CraftingBootstrap.id("asterism_crafting"), stacks()));
        candidates.addAll(hub.byInput(CraftingBootstrap.id("asterism_upgrade"), stacks()));
        candidates.sort(
                Comparator.<net.minecraft.world.item.crafting.Recipe<?>>comparingInt(
                                r -> ((AsterismRecipe) r).priority())
                        .reversed()
                        .thenComparing(r -> r.getId().toString()));
        for (var candidate : candidates) {
            var recipe = (AsterismRecipe) candidate;
            if (!recipe.matches(input(player.getUUID()), server) || !structure(recipe, server))
                continue;
            var craft =
                    new AsterismCraftTask(
                            recipe.getId(),
                            recipe.contentHash(),
                            recipe.displayDuration(),
                            player.getUUID());
            if (!relayRequirements(server, recipe, craft, false)) continue;
            task = craft;
            setChanged();
            return true;
        }
        return false;
    }

    private boolean structure(AsterismRecipe recipe, ServerLevel server) {
        return tier() == AsterismTier.RADIANCE
                || CraftingEnvironment.structure(server, worldPosition, recipe.tier());
    }

    @Override
    protected void tickMachine(ServerLevel server) {
        if (density == null) density = new SkyDensityField(server.getSeed(), 32);
        var balance =
                AsterismEnergy.passive(
                        lumenStored(),
                        carry,
                        lumenCapacity(),
                        server.canSeeSky(worldPosition.above()),
                        worldPosition.getY(),
                        1 - SignSkyService.dayDistributionFactor(server),
                        density.sample(worldPosition.getX(), worldPosition.getZ()));
        setLumenStored(balance.stored());
        carry = balance.carry();
        if (!(task instanceof AsterismCraftTask craft)) return;
        var candidate = CraftingBootstrap.hub(server.getServer()).byId(craft.recipeId());
        if (candidate.isEmpty() || !(candidate.get() instanceof AsterismRecipe recipe)) {
            craft.advance(false, true, true, true, true, true);
            return;
        }
        craft.reconcile(recipe.contentHash(), recipe.displayDuration());
        var input = input(craft.crafter());
        boolean materials = recipe.matchedSlots(input).isPresent();
        boolean phases =
                relayRequirements(server, recipe, craft, true)
                        && processPhases(server, recipe, craft, input);
        craft.advance(
                true,
                materials,
                structure(recipe, server),
                lumenStored() >= recipe.lumen(),
                recipe.gates(input, server),
                phases);
        if (craft.state() == CraftState.FINISHED)
            craft.commit(() -> complete(server, recipe, craft));
        setChanged();
    }

    private boolean processPhases(
            ServerLevel server,
            AsterismRecipe recipe,
            AsterismCraftTask craft,
            AsterismRecipeInput input) {
        var data = craft.recipeData();
        int cursor = Math.max(0, data.getInt("Phase"));
        while (cursor < recipe.phases().size()) {
            var phase = recipe.phases().get(cursor);
            if (craft.progressFraction() < phase.fraction()) break;
            boolean ready =
                    switch (phase.kind()) {
                        case BIND_RELAY, CONSUME_RELAY ->
                                relayRequirements(server, recipe, craft, true);
                        case GATE_CHECK -> recipe.gates(input, server);
                        case EMIT_EFFECT -> {
                            data.put("Effect", phase.payload());
                            yield true;
                        }
                    };
            if (!ready) {
                int wait = data.getInt("PhaseWait") + 1;
                data.putInt("PhaseWait", wait);
                craft.recipeData(data);
                if (wait >= phase.timeout()) craft.abort();
                return false;
            }
            cursor++;
            data.putInt("Phase", cursor);
            data.putInt("PhaseWait", 0);
        }
        craft.recipeData(data);
        return true;
    }

    private boolean complete(ServerLevel server, AsterismRecipe recipe, AsterismCraftTask craft) {
        if (!pending.isEmpty()
                || !recipe.matches(input(craft.crafter()), server)
                || !relayRequirements(server, recipe, craft, true)) return false;
        var changes = new ArrayList<AsterismConsumption.SlotChange>();
        ItemStack output;
        try {
            var snapshot = input(craft.crafter());
            var slots = recipe.matchedSlots(snapshot).orElseThrow();
            for (var entry : slots.entrySet())
                changes.add(
                        AsterismConsumption.simulate(
                                items,
                                entry.getKey(),
                                entry.getValue(),
                                recipe.consume(entry.getKey())));
            for (var requirement : recipe.relays()) {
                var pos = craft.bindings().get(requirement.index());
                if (pos == null
                        || !(server.getBlockEntity(pos) instanceof CraftingRelayBlockEntity relay))
                    return false;
                changes.add(
                        AsterismConsumption.simulate(
                                relay.inventory(), 0, requirement.material(), true));
            }
            output = recipe.assemble(snapshot, server.registryAccess());
        } catch (RuntimeException e) {
            return false;
        }
        if (consumeLumen(recipe.lumen(), true) != recipe.lumen()
                || !AsterismConsumption.apply(changes)) return false;
        server.playSound(
                null,
                worldPosition,
                CraftingContent.CRAFT_FINISH.get(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.7F,
                1F);
        consumeLumen(recipe.lumen(), false);
        pending = output;
        chain = !recipe.flag("no_chain") && !(recipe instanceof AsterismUpgradeRecipe);
        if (recipe instanceof AsterismUpgradeRecipe upgrade)
            server.setBlock(
                    worldPosition,
                    getBlockState().setValue(CraftingContent.TIER, upgrade.target()),
                    3);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new com.mpp.stellaeomphalos.core.platform.CraftCompletedEvent(
                        server,
                        worldPosition,
                        craft.crafter(),
                        recipe.getId(),
                        output,
                        (float)
                                com.mpp.stellaeomphalos.data.codec.RecipeJson.decimal(
                                        recipe.definition(), "experience", 5, 0, 1000000)));
        setChanged();
        markClientDirty();
        return true;
    }

    @Override
    protected void writePersistent(CompoundTag tag) {
        super.writePersistent(tag);
        tag.putLong("LumenCarry", carry);
        tag.putBoolean("Chain", chain);
    }

    @Override
    protected void readPersistent(CompoundTag tag) {
        super.readPersistent(tag);
        carry = Math.max(0, Math.min(999999, tag.getLong("LumenCarry")));
        chain = tag.getBoolean("Chain");
        task = AsterismCraftTask.load(tag.getCompound("Craft")).orElse(null);
    }

    @Override
    protected void readClientState(CompoundTag tag) {
        super.readClientState(tag);
        task = AsterismCraftTask.load(tag.getCompound("Craft")).orElse(null);
    }
}
