package com.mpp.stellaeomphalos.client.codex;

import com.mpp.stellaeomphalos.knowledge.codex.CodexRoute;

import java.util.*;

/** Screen-owned history; overlays do not replace or reinitialize the underlying screen. */
public final class CodexNavigator {
    private final Deque<CodexRoute> back = new ArrayDeque<>(), forward = new ArrayDeque<>();
    private CodexRoute current;

    public Optional<CodexRoute> current() {
        return Optional.ofNullable(current);
    }

    public void push(CodexRoute route) {
        if (route.equals(current)) return;
        if (current != null) {
            if (back.size() == 64) back.removeFirst();
            back.addLast(current);
        }
        current = route;
        forward.clear();
    }

    public void replace(CodexRoute route) {
        current = route;
    }

    public Optional<CodexRoute> back() {
        if (!back.isEmpty()) {
            if (current != null) forward.addLast(current);
            current = back.removeLast();
        }
        return current();
    }

    public Optional<CodexRoute> forward() {
        if (!forward.isEmpty()) {
            if (current != null) back.addLast(current);
            current = forward.removeLast();
        }
        return current();
    }

    public void reset() {
        back.clear();
        forward.clear();
        current = null;
    }

    public int historySize() {
        return back.size();
    }
}
