package com.mpp.stellaeomphalos.client.charge;

import com.mpp.stellaeomphalos.client.OmphalosClient;
import com.mpp.stellaeomphalos.client.event.ClientSessionCleaner;
import com.mpp.stellaeomphalos.network.toClient.PktChargeSync;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-side read-only mirror of starlight charge. Cleanup action ("charge_mirror"):
 * zero the value and reset the session so the first packet of the next session is adopted
 * and late packets from a previous session are dropped (session_id mismatch).
 */
public final class ChargeMirror {
    private static final int NO_SESSION = Integer.MIN_VALUE;
    private static final AtomicBoolean ATTACHED = new AtomicBoolean();
    private static int session = NO_SESSION;
    private static float charge;
    private ChargeMirror() {}

    public static float charge() { return charge; }

    public static void handle(PktChargeSync packet) {
        if (session != NO_SESSION && packet.sessionId() != session) return;
        session = packet.sessionId();
        charge = packet.quantizedCharge() / (float) PktChargeSync.QUANTUM;
    }

    public static void reset() { session = NO_SESSION; charge = 0; }

    public static void attach() {
        if (!ATTACHED.compareAndSet(false, true)) return;
        ClientSessionCleaner.register("charge_mirror", ChargeMirror::reset);
        OmphalosClient.handlers().register(PktChargeSync.class, (minecraft, packet) -> handle(packet));
    }
}
