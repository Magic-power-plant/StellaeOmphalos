package com.mpp.stellaeomphalos.client.render;

import java.lang.reflect.Method;

/** Optional APIs are resolved once; the current pack state is sampled each client tick. */
public final class ShaderCompat {
    private static Method instance, enabled;
    private static boolean probed, active;

    private ShaderCompat() {}

    public static void refresh() {
        if (!probed) {
            probed = true;
            for (String name :
                    new String[] {
                        "net.irisshaders.iris.api.v0.IrisApi", "net.coderbot.iris.api.v0.IrisApi"
                    }) {
                try {
                    var type = Class.forName(name);
                    instance = type.getMethod("getInstance");
                    enabled = type.getMethod("isShaderPackInUse");
                    break;
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        try {
            active = enabled != null && Boolean.TRUE.equals(enabled.invoke(instance.invoke(null)));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            active = false;
        }
    }

    public static boolean shadersInUse() {
        return active
                || com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.flag("render.vanillaShaders");
    }
}
