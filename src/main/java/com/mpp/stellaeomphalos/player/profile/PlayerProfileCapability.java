package com.mpp.stellaeomphalos.player.profile;

import com.mpp.stellaeomphalos.Omphalos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;

/** Capability registration and lifecycle hooks for the authoritative player profile. */
@Mod.EventBusSubscriber(modid = Omphalos.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PlayerProfileCapability {
    public static final Capability<PlayerProfile> PROFILE = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation ID = new ResourceLocation(Omphalos.MODID, "player_profile");
    private PlayerProfileCapability() {}

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) { event.register(PlayerProfile.class); }

    public static void attach(IEventBus ignored) {
        MinecraftForge.EVENT_BUS.addGenericListener(Player.class, event -> {
            if (event instanceof AttachCapabilitiesEvent<?> capabilities) {
                @SuppressWarnings("unchecked")
                AttachCapabilitiesEvent<Player> typed = (AttachCapabilitiesEvent<Player>) capabilities;
                attach(typed);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> clone(event));
    }

    private static void attach(AttachCapabilitiesEvent<Player> event) {
        var provider = new Provider(new DefaultPlayerProfile());
        event.addCapability(ID, provider);
        event.addListener(provider.value::invalidate);
    }

    private static void clone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(PROFILE).ifPresent(old ->
                event.getEntity().getCapability(PROFILE).ifPresent(current -> current.copyFrom(old)));
    }

    public static PlayerProfile get(Player player) {
        return player.getCapability(PROFILE).orElseThrow(() -> new IllegalStateException("Player profile not attached"));
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PlayerProfile profile;
        private final LazyOptional<PlayerProfile> value;
        private Provider(PlayerProfile profile) {
            this.profile = profile;
            this.value = LazyOptional.of(() -> profile);
        }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == PROFILE ? value.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() { return profile.save(); }
        @Override public void deserializeNBT(CompoundTag tag) { profile.load(tag); }
    }
}
