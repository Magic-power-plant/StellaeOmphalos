package com.mpp.stellaeomphalos.constellation.starmap;

import com.mpp.stellaeomphalos.constellation.sign.SignDiscoveryView;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cross-layer bridge: constellation must not import player, so player/boon injects the real
 * discovery view provider here. Until then the default treats every sign as known.
 */
public final class ImprintDiscoverySource {
    public interface Provider {
        SignDiscoveryView discoveryOf(ServerPlayer player);
    }

    private static final SignDiscoveryView ALL_KNOWN = new SignDiscoveryView() {
        @Override public boolean knowsSign(ResourceLocation signId) { return true; }
        @Override public Set<ResourceLocation> knownSigns() {
            return SignRegistry.all().stream().map(sign -> sign.id()).collect(Collectors.toUnmodifiableSet());
        }
    };

    private static volatile Provider provider = player -> ALL_KNOWN;

    private ImprintDiscoverySource() {}

    public static void registerProvider(Provider replacement) { provider = Objects.requireNonNull(replacement); }

    public static SignDiscoveryView of(ServerPlayer player) { return provider.discoveryOf(player); }
}
