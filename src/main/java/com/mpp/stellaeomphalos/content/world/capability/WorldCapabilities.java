package com.mpp.stellaeomphalos.content.world.capability;

import com.mpp.stellaeomphalos.Omphalos;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.*;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nullable;

public final class WorldCapabilities {
    public static final Capability<SpringVeinHolder> SPRING =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<GeodeIndexHolder> GEODES =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<RetroGenStamp> STAMP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private WorldCapabilities() {}

    public static void register(RegisterCapabilitiesEvent e) {
        e.register(SpringVeinHolder.class);
        e.register(GeodeIndexHolder.class);
        e.register(RetroGenStamp.class);
    }

    public static void attach(AttachCapabilitiesEvent<LevelChunk> e) {
        var chunk = e.getObject();
        var spring = new SpringVeinHolder();
        spring.onChanged(() -> chunk.setUnsaved(true));
        var geodes = new GeodeIndexHolder();
        geodes.onChanged(() -> chunk.setUnsaved(true));
        var stamp = new RetroGenStamp();
        stamp.onChanged(() -> chunk.setUnsaved(true));
        add(e, "spring_vein", SPRING, spring);
        add(e, "geode_index", GEODES, geodes);
        add(e, "retrogen_stamp", STAMP, stamp);
    }

    private static <T extends INBTSerializable<CompoundTag>> void add(
            AttachCapabilitiesEvent<LevelChunk> e, String id, Capability<T> cap, T holder) {
        var provider = new Provider<>(cap, holder);
        e.addCapability(new ResourceLocation(Omphalos.MODID, id), provider);
        e.addListener(provider.value::invalidate);
    }

    private static final class Provider<T extends INBTSerializable<CompoundTag>>
            implements ICapabilitySerializable<CompoundTag> {
        private final Capability<T> type;
        private final T holder;
        private final LazyOptional<T> value;

        private Provider(Capability<T> type, T holder) {
            this.type = type;
            this.holder = holder;
            value = LazyOptional.of(() -> holder);
        }

        public <U> LazyOptional<U> getCapability(Capability<U> cap, @Nullable Direction side) {
            return cap == type ? value.cast() : LazyOptional.empty();
        }

        public CompoundTag serializeNBT() {
            return holder.serializeNBT();
        }

        public void deserializeNBT(CompoundTag n) {
            holder.deserializeNBT(n);
        }
    }
}
