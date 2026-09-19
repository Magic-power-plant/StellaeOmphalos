package com.mpp.stellaeomphalos.network;

import net.minecraft.server.MinecraftServer;

public final class SideDispatch {
    private SideDispatch() {}
    public static void assertServerThread(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("World access outside the server thread");
    }
}
