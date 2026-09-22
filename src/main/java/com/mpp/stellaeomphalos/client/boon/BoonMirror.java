package com.mpp.stellaeomphalos.client.boon;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.constellation.boon.BoonNodeLayout;
import com.mpp.stellaeomphalos.constellation.boon.BoonTreeLayout;
import com.mpp.stellaeomphalos.network.toClient.PktBoonDelta;
import com.mpp.stellaeomphalos.network.toClient.PktBoonExp;
import com.mpp.stellaeomphalos.network.toClient.PktBoonTreeSync;
import com.mpp.stellaeomphalos.player.boon.BoonProgress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side read-only mirror of the boon tree and the local player's progress. Feeds
 * {@link BoonTreeLayout} with server-sent node coordinates and exposes an immutable
 * {@link BoonProgress} snapshot through the injected client view. The client renders only; it
 * never evaluates unlock prerequisites.
 *
 * <p>Cleanup action ("boon_mirror"): drop the session marker, layouts, progress bitmaps and the
 * mirror view, so the first packet of the next session is adopted and late packets from a
 * previous session are dropped (session_id mismatch).</p>
 */
public final class BoonMirror {

    private static final int NO_SESSION = Integer.MIN_VALUE;
    private static final AtomicBoolean ATTACHED = new AtomicBoolean();

    private static int session = NO_SESSION;
    private static int treeVersion;
    private static java.util.List<com.mpp.stellaeomphalos.constellation.boon.BoonEdge> edges=java.util.List.of();
    public static java.util.List<com.mpp.stellaeomphalos.constellation.boon.BoonEdge> edges(){return edges;}
    private static final Set<ResourceLocation> APPLIED = new LinkedHashSet<>();
    private static final Set<ResourceLocation> SEALED = new LinkedHashSet<>();
    private static final Map<ResourceLocation, ItemStack> SOCKETED = new HashMap<>();
    private static long exp;
    private static int level = 1;
    private static int availablePoints;
    private static volatile BoonProgress view = empty();

    private BoonMirror() {}

    public static BoonProgress view() {
        return view;
    }

    public static int treeVersion() {
        return treeVersion;
    }

    public static ItemStack socketedIn(ResourceLocation nodeId) {
        return SOCKETED.getOrDefault(nodeId, ItemStack.EMPTY);
    }

    public static void handle(PktBoonTreeSync packet) {
        if (session != NO_SESSION && packet.sessionId() != session) return;
        session = packet.sessionId();
        treeVersion = packet.treeVersion();
        APPLIED.clear();
        SEALED.clear();
        SOCKETED.clear();
        BoonTreeLayout.clear();
        var bySign = new HashMap<ResourceLocation, java.util.List<BoonNodeLayout>>();
        for (var entry : packet.entries()) {
            if (entry.applied()) APPLIED.add(entry.id());
            var signKey = BoonTreeLayout.signKeyOf(entry.id());
            if (signKey != null)
                bySign.computeIfAbsent(signKey, key -> new ArrayList<>())
                        .add(new BoonNodeLayout(entry.id(), entry.x(), entry.z(), entry.kind()));
        }
        bySign.forEach(BoonTreeLayout::applyId);
        var ids=new java.util.HashSet<ResourceLocation>();for(var entry:packet.entries())ids.add(entry.id());
        edges=packet.edges().stream().filter(e->!e.a().equals(e.b())&&ids.contains(e.a())&&ids.contains(e.b()))
                .map(e->new com.mpp.stellaeomphalos.constellation.boon.BoonEdge(e.a(),e.b())).distinct().toList();
        rebuildView();
    }

    public static void handle(PktBoonDelta packet) {
        if (session == NO_SESSION || packet.sessionId() != session) return;
        switch (packet.action()) {
            case PktBoonDelta.ACTION_UNLOCK -> APPLIED.add(packet.id());
            case PktBoonDelta.ACTION_REMOVE -> {
                APPLIED.remove(packet.id());
                SEALED.remove(packet.id());
                SOCKETED.remove(packet.id());
            }
            case PktBoonDelta.ACTION_SEAL -> SEALED.add(packet.id());
            case PktBoonDelta.ACTION_UNSEAL -> SEALED.remove(packet.id());
            case PktBoonDelta.ACTION_SOCKET -> {
                if (packet.extra().contains("Item", Tag.TAG_COMPOUND))
                    SOCKETED.put(packet.id(), ItemStack.of(packet.extra().getCompound("Item")));
                else SOCKETED.remove(packet.id());
            }
            default -> { return; }
        }
        rebuildView();
        if (packet.action()==PktBoonDelta.ACTION_UNLOCK) com.mpp.stellaeomphalos.client.sound.UiSounds.play("boon_unlock");
        if (packet.action()==PktBoonDelta.ACTION_UNSEAL) com.mpp.stellaeomphalos.client.sound.UiSounds.play("boon_seal_break");
    }

    public static void handle(PktBoonExp packet) {
        if (session == NO_SESSION || packet.sessionId() != session) return;
        exp = packet.exp();
        level = packet.level();
        availablePoints = packet.freePoints();
        rebuildView();
    }

    private static void rebuildView() {
        view = BoonProgress.clientMirror(Set.copyOf(APPLIED), Set.copyOf(SEALED), availablePoints, exp, level, null);
    }

    private static BoonProgress empty() {
        return BoonProgress.clientMirror(Set.of(), Set.of(), 0, 0L, 1, null);
    }

    public static void reset() {
        session = NO_SESSION;
        treeVersion = 0;edges=java.util.List.of();
        APPLIED.clear();
        SEALED.clear();
        SOCKETED.clear();
        exp = 0;
        level = 1;
        availablePoints = 0;
        BoonTreeLayout.clear();
        view = empty();
    }

    public static void attach() {
        if (!ATTACHED.compareAndSet(false, true)) return;
        ClientSessionCleaner.register("boon_mirror", BoonMirror::reset);
        BoonProgress.installClientView(player -> view);
        OmphalosClient.handlers().register(PktBoonTreeSync.class, (minecraft, packet) -> handle(packet));
        OmphalosClient.handlers().register(PktBoonDelta.class, (minecraft, packet) -> handle(packet));
        OmphalosClient.handlers().register(PktBoonExp.class, (minecraft, packet) -> handle(packet));
    }
}
