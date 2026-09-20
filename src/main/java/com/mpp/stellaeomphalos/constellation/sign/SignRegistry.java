package com.mpp.stellaeomphalos.constellation.sign;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * Central sign registry. Registration order is the numeric id (network short encoding only;
 * persistence always uses the ResourceLocation). Duplicate names fail fast; after {@link #freeze()}
 * individual registration throws. {@link #rebuild(Collection)} is the sanctioned datapack-reload path.
 */
public final class SignRegistry {
    private static final Map<ResourceLocation, Sign> BY_ID = new LinkedHashMap<>();
    private static final List<Sign> ORDER = new ArrayList<>();
    private static boolean frozen;

    private SignRegistry() {}

    public static synchronized void register(Sign sign) {
        if (frozen) throw new IllegalStateException("SignRegistry is frozen");
        insert(sign);
    }

    /** Replaces all contents atomically; used by the data-table reload path, legal after freeze. */
    public static synchronized void rebuild(Collection<Sign> signs) {
        BY_ID.clear();
        ORDER.clear();
        signs.forEach(SignRegistry::insert);
    }

    private static void insert(Sign sign) {
        if (BY_ID.putIfAbsent(sign.id(), sign) != null)
            throw new IllegalStateException("Duplicate sign id " + sign.id());
        ORDER.add(sign);
        if (sign.signatureItems().isEmpty()) LogUtils.getLogger().warn("Sign {} has no signature items", sign.id());
    }

    public static synchronized void freeze() { frozen = true; }
    public static synchronized boolean frozen() { return frozen; }

    /** Stable within one data reload; changes if datapacks reorder registrations. */
    public static synchronized int numericId(Sign sign) {
        int index = ORDER.indexOf(sign);
        if (index < 0) throw new IllegalArgumentException("Unregistered sign " + sign.id());
        return index;
    }

    public static synchronized @Nullable Sign byId(ResourceLocation id) { return BY_ID.get(id); }

    public static synchronized @Nullable Sign byNumericId(int id) {
        return id >= 0 && id < ORDER.size() ? ORDER.get(id) : null;
    }

    /** Major signs （主星象）, in registration order. */
    public static synchronized List<Sign> majorSigns() { return ORDER.stream().filter(MajorSign.class::isInstance).toList(); }
    /** Ritual-but-not-major signs （从星象）, in registration order. */
    public static synchronized List<Sign> ritualSigns() {
        return ORDER.stream().filter(sign -> sign instanceof RitualSign && !(sign instanceof MajorSign)).toList();
    }
    public static synchronized List<Sign> traitSigns() { return ORDER.stream().filter(TraitSign.class::isInstance).toList(); }
    public static synchronized List<Sign> anomalousSigns() { return ORDER.stream().filter(AnomalousSign.class::isInstance).toList(); }
    public static synchronized List<Sign> all() { return List.copyOf(ORDER); }
}
