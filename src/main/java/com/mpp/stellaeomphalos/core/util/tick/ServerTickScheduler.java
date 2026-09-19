package com.mpp.stellaeomphalos.core.util.tick;

import java.util.function.Consumer;

public final class ServerTickScheduler extends TickScheduler {
    public ServerTickScheduler(Consumer<RuntimeException> errors) { super(errors); }
}
