package com.mpp.stellaeomphalos.core.util.io;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Bounded pure-IO work only. Tasks must never access Level, entities, or SavedData. */
public final class AsyncIoExecutor implements AutoCloseable {
    private static final class Work<T> implements Runnable {
        final CompletableFuture<T> result = new CompletableFuture<>();
        final Supplier<T> supplier;
        Work(Supplier<T> supplier) { this.supplier = supplier; }
        @Override public void run() {
            if (result.isCancelled()) return;
            try { result.complete(supplier.get()); } catch (Throwable exception) { result.completeExceptionally(exception); }
        }
    }
    private final ThreadPoolExecutor executor;
    public AsyncIoExecutor() {
        var counter = new AtomicInteger();
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), action -> {
            var thread = new Thread(action, "stellaeomphalos-io-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }
    public <T> CompletableFuture<T> submit(Supplier<T> work) {
        var pending = new Work<T>(java.util.Objects.requireNonNull(work));
        try { executor.execute(pending); }
        catch (java.util.concurrent.RejectedExecutionException exception) { pending.result.completeExceptionally(exception); }
        return pending.result;
    }
    @Override public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) cancelPending();
        } catch (InterruptedException exception) {
            cancelPending();
            Thread.currentThread().interrupt();
        }
    }
    private void cancelPending() {
        for (var work : executor.shutdownNow()) if (work instanceof Work<?> pending) pending.result.cancel(false);
    }
}
