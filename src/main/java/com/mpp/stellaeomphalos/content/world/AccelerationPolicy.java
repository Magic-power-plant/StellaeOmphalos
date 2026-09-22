package com.mpp.stellaeomphalos.content.world;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/** Failure counts are dimension SavedData, so restarts cannot clear an unsafe ticker. */
public final class AccelerationPolicy extends SavedData {
    private final Map<ResourceLocation, Integer> failures = new HashMap<>();

    public static AccelerationPolicy get(ServerLevel l) {
        return l.getDataStorage()
                .computeIfAbsent(
                        AccelerationPolicy::load,
                        AccelerationPolicy::new,
                        "stellaeomphalos_acceleration");
    }

    public boolean blocked(BlockEntity be) {
        return failures.getOrDefault(ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType()), 0)
                >= 1;
    }

    public void record(BlockEntity be) {
        var key = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(be.getType());
        if (key != null) {
            failures.merge(key, 1, (a, b) -> Math.min(3, a + b));
            setDirty();
        }
    }

    public CompoundTag save(CompoundTag n) {
        var counts = new CompoundTag();
        failures.forEach((id, count) -> counts.putInt(id.toString(), count));
        n.put("Failures", counts);
        return n;
    }

    private static AccelerationPolicy load(CompoundTag n) {
        var result = new AccelerationPolicy();
        var counts = n.getCompound("Failures");
        counts.getAllKeys()
                .forEach(
                        key -> {
                            var id = ResourceLocation.tryParse(key);
                            if (id != null)
                                result.failures.put(id, Math.max(0, counts.getInt(key)));
                        });
        return result;
    }
}
