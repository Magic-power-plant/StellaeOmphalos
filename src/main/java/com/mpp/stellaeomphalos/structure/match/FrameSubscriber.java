package com.mpp.stellaeomphalos.structure.match;

import com.mpp.stellaeomphalos.core.util.world.DimensionPos;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Owns a structure observer's lifecycle; concrete matchers supply resumable domain evaluation. */
public abstract class FrameSubscriber implements AutoCloseable {
    public enum State { UNKNOWN, MATCHED, MISMATCHED, CLOSED }
    public record Evaluation(boolean complete, boolean matching, int work) {
        public Evaluation { if (work < 0) throw new IllegalArgumentException("Negative work"); }
    }
    private final DimensionPos origin;
    private final ResourceLocation pattern;
    private final FrameChangeSet changes = new FrameChangeSet();
    private State state = State.UNKNOWN;
    protected FrameSubscriber(DimensionPos origin, ResourceLocation pattern) { this.origin = origin; this.pattern = pattern; }
    public final DimensionPos origin() { return origin; }
    public final ResourceLocation pattern() { return pattern; }
    public final State state() { return state; }
    public final void invalidate(FrameChangeSet.Change change) {
        if (state == State.CLOSED) return;
        changes.add(change.position(), change.before(), change.after()); state = State.UNKNOWN; restart();
    }
    public final int advance(int budget) {
        if (budget < 1) throw new IllegalArgumentException("Invalid match budget");
        if (state != State.UNKNOWN) return 0;
        var result = evaluate(changes.snapshot(), budget);
        if (result.work() > budget || !result.complete() && result.work() == 0) throw new IllegalStateException("Matcher violated work budget");
        if (result.complete()) { state = result.matching() ? State.MATCHED : State.MISMATCHED; changes.clear(); }
        return result.work();
    }
    protected abstract Evaluation evaluate(List<FrameChangeSet.Change> changes, int budget);
    protected abstract void restart();
    @Override public final void close() { state = State.CLOSED; changes.clear(); restart(); }
}
