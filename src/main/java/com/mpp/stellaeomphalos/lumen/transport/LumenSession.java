package com.mpp.stellaeomphalos.lumen.transport;

import java.util.concurrent.atomic.AtomicInteger;

/** Server-side session counter stamped onto every lumen S2C payload; clients drop stale ids. */
public final class LumenSession {
    private static final AtomicInteger SESSION = new AtomicInteger();
    private LumenSession() {}
    public static int current() { return SESSION.get(); }
    public static void advance() { SESSION.incrementAndGet(); }
}
