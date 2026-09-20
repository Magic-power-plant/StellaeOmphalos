package com.mpp.stellaeomphalos.client.sign;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.constellation.sign.Sign;
import com.mpp.stellaeomphalos.constellation.sign.SignNamingProvider;
import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import com.mpp.stellaeomphalos.constellation.sign.SignSkyAnchor;
import com.mpp.stellaeomphalos.constellation.sign.SignSkyScheduler;
import com.mpp.stellaeomphalos.network.toClient.PktActiveSigns;
import com.mpp.stellaeomphalos.network.toClient.PktSignRename;
import com.mpp.stellaeomphalos.network.toClient.PktSignSkyLayout;
import com.mpp.stellaeomphalos.network.toClient.PktSkySeed;
import com.mpp.stellaeomphalos.network.toServer.PktSkySeedRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Client mirror of the server-authoritative sky state: seeds, active sets, layout and name
 * overrides, keyed by dimension. Every packet carries the server session id; packets older than
 * the adopted session are dropped (AC-2.11). Localized names are generated lazily from the client
 * language, which is read only after the Minecraft instance exists.
 *
 * <p>Session cleanup (registered in {@link #attach()}): clears session id, per-dimension states,
 * name overrides, generated-name cache and the request throttle.</p>
 */
public final class SignSkyMirror {
    /** One active sign as seen by the client; the Sign resolves only when the registry is populated. */
    public record ActiveEntry(int numericId, @Nullable Sign sign, float dist) { }

    private record DimState(long seed, int day, List<ActiveEntry> active, Map<ResourceLocation, SignSkyAnchor> layout) { }
    private record RequestWindow(int count, long lastGameTime) { }

    private static final Map<ResourceKey<Level>, DimState> DIMENSIONS = new HashMap<>();
    private static final Map<ResourceKey<Level>, RequestWindow> REQUESTS = new HashMap<>();
    private static final Map<Integer, Component> NAME_OVERRIDES = new HashMap<>();
    private static final Map<ResourceLocation, String> GENERATED_NAMES = new HashMap<>();
    private static int session = -1;

    private SignSkyMirror() {}

    /** Installed by the integrator inside OmphalosClient's client-setup work. */
    public static void attach() {
        var handlers = OmphalosClient.handlers();
        handlers.register(PktSkySeed.class, (minecraft, packet) -> {
            if (!adopt(packet.sessionId())) return;
            var previous = DIMENSIONS.get(packet.dim());
            DIMENSIONS.put(packet.dim(), new DimState(packet.skySeed(),
                    previous == null ? -1 : previous.day(),
                    previous == null ? List.of() : previous.active(),
                    previous == null ? Map.of() : previous.layout()));
        });
        handlers.register(PktActiveSigns.class, (minecraft, packet) -> {
            if (!adopt(packet.sessionId())) return;
            var active = packet.signs().stream()
                    .map(entry -> new ActiveEntry(entry.sign(), SignRegistry.byNumericId(entry.sign()), entry.dist()))
                    .toList();
            withState(packet.dim(), active, packet.day());
        });
        handlers.register(PktSignSkyLayout.class, (minecraft, packet) -> {
            if (!adopt(packet.sessionId())) return;
            var layout = new HashMap<ResourceLocation, SignSkyAnchor>();
            packet.entries().forEach(entry -> layout.put(entry.sign(), SignSkyAnchor.fromList(entry.anchor())));
            var previous = DIMENSIONS.get(packet.dim());
            DIMENSIONS.put(packet.dim(), new DimState(previous == null ? 0 : previous.seed(),
                    previous == null ? -1 : previous.day(),
                    previous == null ? List.of() : previous.active(), Map.copyOf(layout)));
        });
        handlers.register(PktSignRename.class, (minecraft, packet) -> {
            if (!adopt(packet.sessionId())) return;
            packet.entries().forEach(entry -> NAME_OVERRIDES.put(entry.sign(), entry.name()));
        });
        ClientSessionCleaner.register("sign_sky_mirror", SignSkyMirror::clear);
    }

    /** Session gate: unknown adopts, older drops, newer re-adopts and clears session-scoped data. */
    private static synchronized boolean adopt(int sessionId) {
        if (sessionId < session) return false;
        if (sessionId > session) {
            session = sessionId;
            DIMENSIONS.clear();
            REQUESTS.clear();
            NAME_OVERRIDES.clear();
            GENERATED_NAMES.clear();
        }
        return true;
    }

    private static synchronized void withState(ResourceKey<Level> dim, List<ActiveEntry> active, int day) {
        var previous = DIMENSIONS.get(dim);
        DIMENSIONS.put(dim, new DimState(previous == null ? 0 : previous.seed(), day, active,
                previous == null ? Map.of() : previous.layout()));
    }

    private static synchronized void clear() {
        session = -1;
        DIMENSIONS.clear();
        REQUESTS.clear();
        NAME_OVERRIDES.clear();
        GENERATED_NAMES.clear();
    }

    public static synchronized OptionalLong skySeed(ResourceKey<Level> dim) {
        var state = DIMENSIONS.get(dim);
        return state == null ? OptionalLong.empty() : OptionalLong.of(state.seed());
    }

    public static synchronized List<ActiveEntry> activeSigns(ResourceKey<Level> dim) {
        var state = DIMENSIONS.get(dim);
        return state == null ? List.of() : state.active();
    }

    public static synchronized int day(ResourceKey<Level> dim) {
        var state = DIMENSIONS.get(dim);
        return state == null ? -1 : state.day();
    }

    public static synchronized Map<ResourceLocation, SignSkyAnchor> layout(ResourceKey<Level> dim) {
        var state = DIMENSIONS.get(dim);
        return state == null ? Map.of() : state.layout();
    }

    public static synchronized int session() { return session; }

    /**
     * Localized random name for a sign: server rename override first, otherwise generated from the
     * client language and the dimension's sky seed. Must not be called before a Minecraft instance
     * exists; the language code is resolved lazily on first use.
     */
    public static synchronized Component displayName(ResourceKey<Level> dim, int numericId, ResourceLocation signId) {
        var override = NAME_OVERRIDES.get(numericId);
        if (override != null) return override;
        String cached = GENERATED_NAMES.get(signId);
        if (cached != null) return Component.literal(cached);
        String language = Minecraft.getInstance().getLanguageManager().getSelected();
        long seed = skySeed(dim).orElse(0L);
        String generated = SignNamingProvider.generateName(language, seed ^ signId.hashCode() * 0x9E3779B97F4A7C15L);
        GENERATED_NAMES.put(signId, generated);
        return Component.literal(generated);
    }

    /** Requests the sky seed for a dimension; client-side throttle mirrors the server-side limit. */
    public static synchronized boolean requestSeed(ResourceKey<Level> dim) {
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        long now = level.getGameTime();
        var window = REQUESTS.get(dim);
        if (window != null && (window.count() >= SkySeedSessionLimits.MAX
                || (window.count() > 0 && now - window.lastGameTime() < SkySeedSessionLimits.INTERVAL))) return false;
        REQUESTS.put(dim, new RequestWindow(window == null ? 1 : window.count() + 1, now));
        return OmphalosClient.sendDependent(new PktSkySeedRequest(session, dim));
    }

    /** Local mirror of the server-side rate-limit constants (see SkySeedSession). */
    private static final class SkySeedSessionLimits {
        private static final int MAX = 3;
        private static final int INTERVAL = 40;
    }
}
