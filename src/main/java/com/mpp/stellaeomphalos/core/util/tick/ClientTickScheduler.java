package com.mpp.stellaeomphalos.core.util.tick;

import java.util.function.Consumer;

public final class ClientTickScheduler extends TickScheduler {
    public ClientTickScheduler(Consumer<RuntimeException> errors) { super(errors); }
    public long wallTick() { return System.nanoTime() / 50000000L; }
}
