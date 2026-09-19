package com.mpp.stellaeomphalos.core.util.tick;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

/** Main-thread confined lifecycle. Tasks submitted during dispatch run no earlier than the next tick. */
public abstract class TickScheduler implements AutoCloseable {
    private record Task(long due, long sequence, Runnable action) {}
    private final PriorityQueue<Task> tasks = new PriorityQueue<>(Comparator.comparingLong(Task::due).thenComparingLong(Task::sequence));
    private final Queue<Task> incoming = new ArrayDeque<>();
    private final Consumer<RuntimeException> errors;
    private long tick;
    private long sequence;
    private boolean closed;
    protected TickScheduler(Consumer<RuntimeException> errors) { this.errors = errors; }
    public final void schedule(long delay, Runnable action) {
        if (closed) throw new IllegalStateException("Scheduler closed");
        if (delay < 0) throw new IllegalArgumentException("Negative delay");
        incoming.add(new Task(Math.addExact(tick, Math.max(1, delay)), sequence++, java.util.Objects.requireNonNull(action)));
    }
    public final int advance(int budget) {
        if (closed) throw new IllegalStateException("Scheduler closed");
        if (budget < 1) throw new IllegalArgumentException("Empty budget");
        tick++;
        tasks.addAll(incoming);
        incoming.clear();
        int executed = 0;
        while (executed < budget && !tasks.isEmpty() && tasks.peek().due <= tick) {
            var task = tasks.remove();
            try { task.action.run(); } catch (RuntimeException exception) { errors.accept(exception); }
            executed++;
        }
        return executed;
    }
    public final long currentTick() { return tick; }
    public final int pendingCount() { return tasks.size() + incoming.size(); }
    @Override public final void close() { closed = true; tasks.clear(); incoming.clear(); }
}
