package com.mpp.stellaeomphalos.client.screen;

import com.mpp.stellaeomphalos.constellation.starmap.StarLine;
import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;

import java.util.*;

/** Drawing state is independent of the screen and never mutates authoritative progress. */
public final class SignCanvas {
    private final List<StarPoint> stars;
    private final LinkedHashSet<StarLine> drawn = new LinkedHashSet<>();
    private StarPoint start;
    private final Set<StarLine> view = Collections.unmodifiableSet(drawn);

    public SignCanvas(List<StarPoint> stars) {
        this.stars = List.copyOf(stars);
    }

    public StarPoint nearest(double x, double y, double radius) {
        StarPoint found = null;
        double best = radius * radius;
        for (var p : stars) {
            double d = (p.x() - x) * (p.x() - x) + (p.y() - y) * (p.y() - y);
            if (d <= best) {
                best = d;
                found = p;
            }
        }
        return found;
    }

    public void begin(double x, double y, double radius) {
        start = nearest(x, y, radius);
    }

    public boolean release(double x, double y, double radius) {
        var end = nearest(x, y, radius);
        var from = start;
        start = null;
        return from != null
                && end != null
                && !from.equals(end)
                && drawn.add(new StarLine(from, end));
    }

    public void undo() {
        if (drawn.isEmpty()) return;
        var last = drawn.stream().reduce((a, b) -> b).orElseThrow();
        drawn.remove(last);
    }

    public void clear() {
        start = null;
        drawn.clear();
    }

    public List<StarPoint> stars() {
        return stars;
    }

    public Set<StarLine> lines() {
        return view;
    }

    public boolean matches(Collection<StarLine> template) {
        return !template.isEmpty()
                && template.size() == drawn.size()
                && new HashSet<>(template).equals(drawn);
    }
}
