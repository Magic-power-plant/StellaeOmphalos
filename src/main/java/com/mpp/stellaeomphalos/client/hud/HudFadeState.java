package com.mpp.stellaeomphalos.client.hud;

/** Tick-based HUD fade; repeated requests extend hold without restarting the transition. */
public final class HudFadeState {
    private int remaining;
    private float alpha;

    public void request(int ticks) {
        remaining = Math.max(remaining, ticks);
    }

    public void tick() {
        if (remaining > 0) remaining--;
        float target = remaining >= 15 ? 1 : remaining / 15F;
        alpha = Math.max(0, Math.min(target, alpha + 1F / 15));
    }

    public float alpha() {
        return alpha;
    }

    public void clear() {
        remaining = 0;
        alpha = 0;
    }
}
