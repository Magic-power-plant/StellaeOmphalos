package com.mpp.stellaeomphalos.client.render;

import com.mpp.stellaeomphalos.lumen.fluid.MoltenLumenFluidType;

public final class MoltenLumenClientSetup {
    private MoltenLumenClientSetup() {}

    /**
     * [待验证] 契约要求挂 RegisterClientExtensionsEvent，但 Forge 47.4.23（1.20.1）尚无此事件； 1.20.1 口径为
     * FluidType#initializeClient。由集成人在 OmphalosClient.setup 的 enqueueWork 内调用本方法，完成 FluidType
     * 构造期捕获的扩展注册回调。
     */
    public static void registerExtensions() {
        MoltenLumenFluidType.setClientExtensions(new MoltenLumenClientExtensions());
    }
}
