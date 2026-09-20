package com.mpp.stellaeomphalos.crafting.altar.recipe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Server-thread lifecycle owner. Recipes never carry progress; finish is a one-shot commit token.
 */
public abstract class AbstractCraftTask {
    private final ResourceLocation recipeId;
    private final UUID crafter;
    private long hash;
    private int ticks;
    private int total;
    private int stalled;
    private CraftState state = CraftState.RUNNING;

    protected AbstractCraftTask(ResourceLocation recipeId, long hash, int total, UUID crafter) {
        if (total < 1) throw new IllegalArgumentException("Nonpositive duration");
        this.recipeId = recipeId;
        this.hash = hash;
        this.total = total;
        this.crafter = crafter;
    }

    public final ResourceLocation recipeId() {
        return recipeId;
    }

    public final UUID crafter() {
        return crafter;
    }

    public final CraftState state() {
        return state;
    }

    public final int ticks() {
        return ticks;
    }

    public final int totalTicks() {
        return total;
    }

    public final double progressFraction() {
        return (double) ticks / total;
    }

    public final void reconcile(long newHash, int duration) {
        if (duration < 1) throw new IllegalArgumentException("Nonpositive duration");
        if (hash != newHash || total != duration) {
            ticks = (int) Math.min(duration, (long) ticks * duration / total);
            total = duration;
            hash = newHash;
            onRecipeChanged();
        }
        if (state == CraftState.ORPHANED) {
            state = CraftState.RUNNING;
            stalled = 0;
        }
    }

    protected void onRecipeChanged() {}

    public final boolean advance(
            boolean exists,
            boolean materials,
            boolean structure,
            boolean energy,
            boolean gate,
            boolean phase) {
        if (state == CraftState.IDLE || state == CraftState.FINISHED) return false;
        if (!exists) {
            state = CraftState.ORPHANED;
            if (++stalled >= 200) abort();
            return false;
        }
        if (!materials) {
            abort();
            return false;
        }
        if (!structure) {
            state = CraftState.SUSPENDED;
            if (++stalled >= 200) abort();
            return false;
        }
        if (!energy || !gate) {
            state = CraftState.PAUSED;
            stalled = 0;
            return false;
        }
        if (!phase) {
            if (++stalled >= 100) state = CraftState.SUSPENDED;
            if (stalled >= 300) abort();
            return false;
        }
        stalled = 0;
        state = CraftState.RUNNING;
        if (++ticks >= total) {
            ticks = total;
            state = CraftState.FINISHED;
        }
        return true;
    }

    public final void abort() {
        state = CraftState.IDLE;
    }

    public final boolean commit(java.util.function.BooleanSupplier transaction) {
        if (state != CraftState.FINISHED) return false;
        state = CraftState.IDLE;
        return transaction.getAsBoolean();
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("Recipe", recipeId.toString());
        tag.putLong("Hash", hash);
        tag.putUUID("Crafter", crafter);
        tag.putInt("Tick", ticks);
        tag.putInt("Total", total);
        tag.putInt("Stalled", stalled);
        tag.putString("State", (state == CraftState.FINISHED ? CraftState.RUNNING : state).name());
        return tag;
    }

    protected final void restore(CompoundTag tag) {
        ticks = Math.max(0, Math.min(total - 1, tag.getInt("Tick")));
        stalled = Math.max(0, Math.min(300, tag.getInt("Stalled")));
        try {
            state = CraftState.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            state = CraftState.RUNNING;
        }
        if (state == CraftState.FINISHED) state = CraftState.RUNNING;
    }
}
