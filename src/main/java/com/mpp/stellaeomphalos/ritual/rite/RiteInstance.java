package com.mpp.stellaeomphalos.ritual.rite;

import com.mpp.stellaeomphalos.structure.match.StructureState;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Proportional charging, resumable warmup and persisted settlement prevent reload duplication. */
public final class RiteInstance extends AbstractRiteInstance {
    private final ResourceLocation recipeId;
    private final List<ItemStack> pending = new ArrayList<>();
    private boolean settlementPrepared, requested;

    public RiteInstance(ResourceLocation id) {
        recipeId = id;
    }

    public ResourceLocation recipeId() {
        return recipeId;
    }

    public void requestStart() {
        requested = true;
        failures = 0;
    }

    public void reset(RiteHost host) {
        requested = true;
        failures = 0;
        transition(RiteState.SCANNING, host);
    }

    public int effectiveCycle(RiteRecipe recipe) {
        return (int) Math.ceil(recipe.cycleTicks() * (1 + 0.1 * Math.min(interrupts, 5)));
    }

    public List<ItemStack> pendingOutputs() {
        return pending.stream().map(ItemStack::copy).toList();
    }

    public void tick(RiteRecipe recipe, RiteHost host, RandomSource random) {
        if (recipe == null) {
            transition(RiteState.IDLE, host);
            return;
        }
        if (cooldown > 0 && !started) {
            cooldown--;
            return;
        }
        var structure = host.structureState();
        boolean broken =
                recipe.requiresStructureIntact()
                        && (structure == StructureState.BROKEN
                                || structure == StructureState.LOCKED);
        boolean hard = broken || !host.crystalValid(recipe);
        boolean soft =
                structure == StructureState.INDETERMINATE
                        || !host.celestialReady(recipe)
                        || !host.amplifiersReady(recipe)
                        || host.storedLumen() < recipe.passiveLumenThreshold();
        if (state == RiteState.LOCKED) return;
        if (started && hard) {
            if (state != RiteState.INTERRUPTED) {
                resumeState = state == RiteState.WARMUP ? RiteState.WARMUP : RiteState.RUNNING;
                interrupts++;
                transition(RiteState.INTERRUPTED, host);
            }
            if (++interruptedTicks >= 200 && broken) transition(RiteState.LOCKED, host);
            return;
        }
        if (state == RiteState.INTERRUPTED && !hard) {
            interruptedTicks = 0;
            transition(soft ? RiteState.STALLED : resumeState, host);
            return;
        }
        if (started && soft && state != RiteState.SCANNING) {
            if (state != RiteState.STALLED) {
                if (com.mpp.stellaeomphalos.OmphalosConfig.SERVER.flag(
                        "ritual.progressResetOnStall")) {
                    progress = 0;
                    consumedLumen = 0;
                }
                resumeState =
                        state == RiteState.WARMUP
                                ? RiteState.WARMUP
                                : state == RiteState.FINISHING
                                        ? RiteState.FINISHING
                                        : RiteState.RUNNING;
                transition(RiteState.STALLED, host);
            }
            return;
        }
        switch (state) {
            case IDLE -> {
                if (requested || failures < 3) {
                    transition(RiteState.SCANNING, host);
                }
            }
            case SCANNING -> {
                if (broken) {
                    transition(RiteState.LOCKED, host);
                } else if (hard || soft) {
                    failures++;
                    cooldown = 20;
                    transition(started ? RiteState.STALLED : RiteState.IDLE, host);
                } else {
                    failures = 0;
                    transition(
                            settlementPrepared
                                    ? RiteState.FINISHING
                                    : started
                                            ? (warmup > 0 ? RiteState.WARMUP : RiteState.RUNNING)
                                            : RiteState.READY,
                            host);
                }
            }
            case READY -> {
                if (hard || soft) {
                    transition(RiteState.SCANNING, host);
                    break;
                }
                if (requested) {
                    requested = false;
                    int charge = (recipe.lumenPerCycle() + 19) / 20;
                    if (host.storedLumen() < charge) break;
                    host.consumeLumen(charge);
                    consumedLumen += charge;
                    warmupPaid = true;
                    started = true;
                    warmup = 20;
                    transition(RiteState.WARMUP, host);
                }
            }
            case WARMUP -> {
                if (--warmup <= 0) transition(RiteState.RUNNING, host);
            }
            case RUNNING -> {
                int total = effectiveCycle(recipe);
                int next = Math.min(total, progress + 1);
                int due =
                        Math.max(
                                0,
                                (int) ((long) recipe.lumenPerCycle() * next / total)
                                        - consumedLumen);
                int upkeep = host.amplifierUpkeep();
                if (host.storedLumen() < due + upkeep) {
                    resumeState = RiteState.RUNNING;
                    transition(RiteState.STALLED, host);
                    break;
                }
                host.consumeLumen(due + upkeep);
                consumedLumen += due;
                progress = next;
                if (progress >= total) transition(RiteState.FINISHING, host);
            }
            case STALLED -> {
                if (!soft && !hard)
                    transition(settlementPrepared ? RiteState.FINISHING : resumeState, host);
            }
            case FINISHING -> {
                if (!settlementPrepared) {
                    long total =
                            recipe.outputs().stream()
                                    .mapToLong(RiteRecipe.WeightedOutput::weight)
                                    .sum();
                    if (total > 0) {
                        long draw = (long) (random.nextDouble() * total);
                        for (var output : recipe.outputs())
                            if ((draw -= output.weight()) < 0) {
                                pending.add(output.create(random));
                                break;
                            }
                    }
                    settlementPrepared = true;
                    revision++;
                }
                if (host.commitOutputs(pendingOutputs())) {
                    pending.clear();
                    settlementPrepared = false;
                    cycles++;
                    progress = consumedLumen = interrupts = 0;
                    host.cycleCompleted();
                    warmupPaid = false;
                    transition(recipe.repeating() ? RiteState.RUNNING : RiteState.READY, host);
                    if (!recipe.repeating()) started = false;
                }
            }
            default -> {}
        }
    }

    public CompoundTag save() {
        var n = new CompoundTag();
        saveLifecycle(n);
        n.putString("Rite", recipeId.toString());
        n.putBoolean("SettlementPrepared", settlementPrepared);
        var list = new ListTag();
        pending.forEach(s -> list.add(s.save(new CompoundTag())));
        n.put("PendingOutputs", list);
        return n;
    }

    public static RiteInstance load(CompoundTag n) {
        var id = ResourceLocation.tryParse(n.getString("Rite"));
        if (id == null) throw new IllegalArgumentException("Invalid rite id");
        var r = new RiteInstance(id);
        r.readLifecycle(n);
        r.settlementPrepared = n.getBoolean("SettlementPrepared");
        for (var entry : n.getList("PendingOutputs", Tag.TAG_COMPOUND))
            if (r.pending.size() < 64) {
                var stack = ItemStack.of((CompoundTag) entry);
                if (!stack.isEmpty()) r.pending.add(stack);
            }
        return r;
    }
}
