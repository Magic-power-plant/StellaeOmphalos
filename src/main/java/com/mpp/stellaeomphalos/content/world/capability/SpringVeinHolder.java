package com.mpp.stellaeomphalos.content.world.capability;

import com.mpp.stellaeomphalos.content.world.SpringFluidEntry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;

/** Persisted allocation wins over datapack changes; exhaustion never causes a reroll. */
public final class SpringVeinHolder implements SpringVein, INBTSerializable<CompoundTag> {
    public enum State {
        UNINITIALIZED,
        PRESENT,
        NO_SPRING,
        INVALID
    }

    private State state = State.UNINITIALIZED;
    private ResourceLocation fluid = new ResourceLocation("minecraft", "empty");
    private String legacy = "";
    private int capacity, remaining;
    private long seedHash;
    private Runnable changed = () -> {};

    public void onChanged(Runnable callback) {
        changed = callback;
    }

    public State state() {
        return state;
    }

    public static long mix64(long x) {
        x ^= x >>> 30;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        x *= 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    public static long seed(long world, int x, int z) {
        return mix64(
                mix64(mix64(mix64(world) ^ (0x9E3779B97F4A7C15L * x)) ^ (0xC2B2AE3D27D4EB4FL * z))
                        ^ 0x535052494E47L);
    }

    public void initialize(long world, int x, int z, List<SpringFluidEntry> table) {
        if (state != State.UNINITIALIZED) return;
        seedHash = seed(world, x, z);
        var rng = new Random(seedHash);
        int warmup = rng.nextInt(8) + 4;
        for (int i = 0; i < warmup; i++) rng.nextLong();
        var sorted =
                table.stream()
                        .filter(e -> e.weight() > 0)
                        .sorted(
                                Comparator.comparing((SpringFluidEntry e) -> e.fluid().toString())
                                        .thenComparingInt(SpringFluidEntry::guaranteedMb)
                                        .thenComparingInt(SpringFluidEntry::extraRandomMb)
                                        .thenComparingDouble(SpringFluidEntry::weight)
                                        .thenComparing(SpringFluidEntry::temperatureClass)
                                        .thenComparing(SpringFluidEntry::requiredMod))
                        .toList();
        double total = sorted.stream().mapToDouble(SpringFluidEntry::weight).sum();
        state = State.NO_SPRING;
        if (total > 0) {
            double draw = rng.nextDouble() * total;
            var chosen = sorted.get(sorted.size() - 1);
            for (var e : sorted)
                if ((draw -= e.weight()) < 0) {
                    chosen = e;
                    break;
                }
            fluid = chosen.fluid();
            capacity =
                    chosen.guaranteedMb()
                            + (chosen.extraRandomMb() == 0
                                    ? 0
                                    : rng.nextInt(chosen.extraRandomMb() + 1));
            remaining = capacity;
            state = State.PRESENT;
        }
        changed.run();
    }

    private void ready() {
        if (state == State.UNINITIALIZED)
            throw new IllegalStateException("Spring has not initialized on server thread");
    }

    public boolean present() {
        return state == State.PRESENT && remaining > 0;
    }

    public ResourceLocation fluidId() {
        ready();
        return fluid;
    }

    public int capacityMb() {
        ready();
        return capacity;
    }

    public int remainingMb() {
        ready();
        return remaining;
    }

    public int drain(int amount, boolean consume) {
        ready();
        int n = present() ? Math.min(Math.max(0, amount), remaining) : 0;
        if (consume && n > 0) {
            remaining -= n;
            changed.run();
        }
        return n;
    }

    public void invalidate() {
        legacy = fluid.toString();
        state = State.INVALID;
        capacity = remaining = 0;
        changed.run();
    }

    public CompoundTag serializeNBT() {
        var n = new CompoundTag();
        n.putInt("Version", 1);
        n.putString("State", state.name());
        n.putString("Fluid", fluid.toString());
        n.putString("LegacyFluid", legacy);
        n.putInt("Capacity", capacity);
        n.putInt("Remaining", remaining);
        n.putLong("SeedHash", seedHash);
        return n;
    }

    public void deserializeNBT(CompoundTag n) {
        if (n.isEmpty()) return;
        try {
            state = State.valueOf(n.getString("State"));
        } catch (IllegalArgumentException e) {
            state = State.INVALID;
        }
        fluid = ResourceLocation.tryParse(n.getString("Fluid"));
        if (fluid == null) fluid = new ResourceLocation("minecraft", "empty");
        legacy = n.getString("LegacyFluid");
        capacity = Math.max(0, n.getInt("Capacity"));
        remaining = Math.max(0, Math.min(capacity, n.getInt("Remaining")));
        seedHash = n.getLong("SeedHash");
        if (n.getInt("Version") > 1) invalidate();
    }

    public void validateFluid() {
        if (state == State.PRESENT
                && !net.minecraftforge.registries.ForgeRegistries.FLUIDS.containsKey(fluid))
            invalidate();
    }
}
