package com.mpp.stellaeomphalos.client.view;

import java.util.List;

/** Immutable keyframes and separate elapsed lifetime, with smooth positional interpolation. */
public final class ViewSequence extends AbstractViewSequence {
    public record Keyframe(
            double x, double y, double z, int tick, float yaw, float pitch, float fov) {
        public Keyframe {
            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(z)
                    || tick < 0
                    || !Float.isFinite(yaw)
                    || !Float.isFinite(pitch)
                    || fov < 20
                    || fov > 120) throw new IllegalArgumentException("Invalid camera keyframe");
        }
    }

    private final List<Keyframe> path;
    private float yaw, pitch, fov;

    public ViewSequence(List<Keyframe> path, int priority) {
        super(duration(path), priority);
        this.path = List.copyOf(path);
        for (int i = 1; i < path.size(); i++)
            if (path.get(i).tick() <= path.get(i - 1).tick())
                throw new IllegalArgumentException("Unordered camera path");
    }

    private static int duration(List<Keyframe> path) {
        if (path.size() < 2 || path.size() > 128 || path.get(0).tick() != 0)
            throw new IllegalArgumentException("Invalid camera path");
        return path.get(path.size() - 1).tick();
    }

    public void sample(float partial, org.joml.Vector3d out) {
        double time = age + Math.max(0, Math.min(1, partial));
        int index = 1;
        while (index < path.size() - 1 && path.get(index).tick() < time) index++;
        var a = path.get(index - 1);
        var b = path.get(index);
        double t = Math.max(0, Math.min(1, (time - a.tick()) / (b.tick() - a.tick())));
        t = t * t * (3 - 2 * t);
        out.set(
                a.x() + (b.x() - a.x()) * t,
                a.y() + (b.y() - a.y()) * t,
                a.z() + (b.z() - a.z()) * t);
        yaw = net.minecraft.util.Mth.rotLerp((float) t, a.yaw(), b.yaw());
        pitch = (float) (a.pitch() + (b.pitch() - a.pitch()) * t);
        fov = (float) (a.fov() + (b.fov() - a.fov()) * t);
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float fov() {
        return fov;
    }
}
