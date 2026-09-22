package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.network.toClient.PktDomainParticle;

import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-side queue of domain particle requests (rendering data for Part-7; this part performs no
 * rendering). Parameters are stored verbatim and drained by the Part-7 consumer each frame. Cleanup
 * action ("domain_particle_mirror"): drop the queue and reset the session so late packets from a
 * previous session are discarded.
 */
public final class DomainParticleMirror {
    public record Request(int type, BlockPos pos, BlockPos target, long seed) {}

    private static final int NO_SESSION = Integer.MIN_VALUE;
    private static final int CAP = 512;
    private static final AtomicBoolean ATTACHED = new AtomicBoolean();
    private static final Deque<Request> QUEUE = new ArrayDeque<>();
    private static int session = NO_SESSION;

    private DomainParticleMirror() {}

    public static void handle(PktDomainParticle packet) {
        if (session != NO_SESSION && packet.sessionId() != session) return;
        session = packet.sessionId();
        synchronized (QUEUE) {
            while (QUEUE.size() >= CAP) QUEUE.pollFirst();
            QUEUE.addLast(
                    new Request(
                            packet.type(),
                            BlockPos.of(packet.pos()),
                            packet.target().map(BlockPos::of).orElse(null),
                            packet.seed()));
        }
    }

    /** Part-7 drains this once per frame; the list is cleared in the process. */
    public static List<Request> drain() {
        synchronized (QUEUE) {
            var out = new ArrayList<Request>(QUEUE);
            QUEUE.clear();
            return out;
        }
    }

    public static void reset() {
        synchronized (QUEUE) {
            QUEUE.clear();
        }
        session = NO_SESSION;
    }

    /** Integrator hook: call from {@code OmphalosClient.setup} enqueueWork. */
    public static void attach() {
        if (!ATTACHED.compareAndSet(false, true)) return;
        ClientSessionCleaner.register("domain_particle_mirror", DomainParticleMirror::reset);
        OmphalosClient.handlers()
                .register(PktDomainParticle.class, (minecraft, packet) -> handle(packet));
    }
}
