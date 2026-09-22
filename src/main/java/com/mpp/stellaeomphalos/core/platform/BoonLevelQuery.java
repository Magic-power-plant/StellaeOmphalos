package com.mpp.stellaeomphalos.core.platform;

import java.util.function.ToIntFunction;
import net.minecraft.server.level.ServerPlayer;

/**
 * 星眷等级的跨层只读查询点。
 *
 * <p>进度触发器位于 `knowledge` 层，而星眷进度位于 `player` 层；两层之间禁止直接引用。因此本类把
 * "读取玩家星眷等级"抽象为一个可安装的读取器：`player` 层在装配期注入实现，`knowledge` 层只依赖本类。
 * 未安装时返回 0，保证专用服务器在没有星眷系统时进度判据仍然安全。
 */
public final class BoonLevelQuery {

    private static volatile ToIntFunction<ServerPlayer> reader = player -> 0;

    private BoonLevelQuery() {}

    /** 由 `player` 层在装配期注入实现；重复安装以后一次为准。 */
    public static void install(ToIntFunction<ServerPlayer> implementation) {
        reader = implementation == null ? player -> 0 : implementation;
    }

    /** @return 玩家当前星眷等级；未安装实现时返回 0 */
    public static int level(ServerPlayer player) {
        return reader.applyAsInt(player);
    }
}
