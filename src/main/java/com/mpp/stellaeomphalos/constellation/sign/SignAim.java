package com.mpp.stellaeomphalos.constellation.sign;

/** Shared projection gate; accepts a 35 degree cone around the sign patch centre. */
public final class SignAim {
    private SignAim() {}

    public static boolean aligned(SignSkyAnchor anchor, float yaw, float pitch) {
        if (anchor == null || pitch > -45) return false;
        double x = anchor.baseX() + 15 * (anchor.incUx() + anchor.incVx());
        double y = anchor.baseY() + 15 * (anchor.incUy() + anchor.incVy());
        double z = anchor.baseZ() + 15 * (anchor.incUz() + anchor.incVz());
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1e-9) return false;
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double dot =
                (-Math.sin(yr) * Math.cos(pr) * x
                                - Math.sin(pr) * y
                                + Math.cos(yr) * Math.cos(pr) * z)
                        / length;
        return dot >= Math.cos(Math.toRadians(35));
    }
}
