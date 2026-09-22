package com.mpp.stellaeomphalos.client.effect;

import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

import java.util.ArrayList;

/** Stable, global priority budget. Pending spawns are never iterated by the current tick. */
public final class EffectDirector {
    private final ArrayList<EffectTrack> active = new ArrayList<>(), pending = new ArrayList<>();
    private int budget = 512, dropped;
    private boolean ticking;

    public void budget(int value) {
        budget = Math.max(0, value);
        trim();
    }

    public <T extends EffectTrack> T spawn(T track) {
        pending.add(track);
        trim();
        return track;
    }

    private void trim() {
        if (!ticking) {
            active.removeIf(EffectTrack::isExpired);
            pending.removeIf(EffectTrack::isExpired);
        }
        int optional = 0, mandatory = 0;
        for (var t : active) if (!t.isMandatory() && !t.isExpired()) optional++;
        for (var t : pending) if (!t.isMandatory() && !t.isExpired()) optional++;
        for (var t : active) if (t.isMandatory() && !t.isExpired()) mandatory++;
        for (var t : pending) if (t.isMandatory() && !t.isExpired()) mandatory++;
        while (optional > Math.max(0, budget - mandatory)) {
            EffectTrack victim = null;
            for (var t : active)
                if (!t.isMandatory()
                        && !t.isExpired()
                        && (victim == null || t.priority() < victim.priority())) victim = t;
            for (var t : pending)
                if (!t.isMandatory()
                        && !t.isExpired()
                        && (victim == null || t.priority() < victim.priority())) victim = t;
            if (victim == null) break;
            victim.expire();
            if (!ticking) {
                active.remove(victim);
                pending.remove(victim);
            }
            optional--;
            dropped++;
        }
    }

    public void tickAll() {
        active.addAll(pending);
        pending.clear();
        ticking = true;
        try {
            for (int i = 0, end = active.size(); i < end; i++)
                if (!active.get(i).isExpired()) active.get(i).tick();
        } finally {
            ticking = false;
            active.removeIf(EffectTrack::isExpired);
            pending.removeIf(EffectTrack::isExpired);
            trim();
        }
    }

    public void flush(EffectLane lane, WorldDraw draw, double distanceSq) {
        flush(lane, draw, distanceSq, true);
    }

    public void flush(EffectLane lane, WorldDraw draw, double distanceSq, boolean decorations) {
        for (int i = 0; i < active.size(); i++) {
            var t = active.get(i);
            if (t.lane() == lane
                    && (decorations || t.isMandatory())
                    && !t.isExpired()
                    && t.distanceSq(draw.cameraX, draw.cameraY, draw.cameraZ) <= distanceSq)
                t.collect(draw);
        }
    }

    public void clearAll() {
        for (var t : active) t.expire();
        for (var t : pending) t.expire();
        active.clear();
        pending.clear();
        dropped = 0;
    }

    public int trackCount() {
        return active.size() + pending.size();
    }

    public int dropped() {
        return dropped;
    }
}
