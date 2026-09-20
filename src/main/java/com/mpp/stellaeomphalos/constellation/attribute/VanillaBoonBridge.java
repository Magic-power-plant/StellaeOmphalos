package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** 8 个原版桥接属性的挂载/摘除/刷新。挂载时机：实体进入世界（登录/重生/维度切换/磁盘载入均触发）；
 *  刷新由 Provider 失效驱动（BoonValueBridge.invalidate → 批量重建），不在 tick 中做。 */
public final class VanillaBoonBridge {

    private static volatile List<VanillaBoonAttribute> bridged = List.of();

    private VanillaBoonBridge() {}

    static void init(List<VanillaBoonAttribute> attributes) {
        bridged = attributes;
        BoonValueBridge.addInvalidationListener(VanillaBoonBridge::onInvalidate);
    }

    public static List<VanillaBoonAttribute> bridged() {
        return bridged;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Player player) attach(player);
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Player player) detach(player);
    }

    public static void attach(Player player) {
        for (var owner : bridged)
            for (var attribute : owner.vanillaAttributes()) {
                var instance = player.getAttribute(attribute);
                if (instance == null) continue;
                sanitize(instance, owner);
                for (var mode : BoonModifier.Mode.values())
                    instance.addTransientModifier(owner.createModifier(player, mode));
            }
        clampHealth(player);
    }

    public static void detach(Player player) {
        for (var owner : bridged)
            for (var attribute : owner.vanillaAttributes()) {
                var instance = player.getAttribute(attribute);
                if (instance == null) continue;
                for (var mode : BoonModifier.Mode.values())
                    instance.removeModifier(DynamicBoonModifier.modifierId(owner, mode));
            }
        clampHealth(player);
    }

    /** 重建全部桥接修饰符以标脏原版实例（触发重算与客户端同步），O(属性数)。 */
    public static void refreshAll(Player player) {
        for (var owner : bridged)
            for (var attribute : owner.vanillaAttributes()) {
                var instance = player.getAttribute(attribute);
                if (instance == null) continue;
                sanitize(instance, owner);
                for (var mode : BoonModifier.Mode.values()) {
                    instance.removeModifier(DynamicBoonModifier.modifierId(owner, mode));
                    instance.addTransientModifier(owner.createModifier(player, mode));
                }
            }
        clampHealth(player);
    }

    /** 剔除同名但 UUID 不同的占位修饰符（持久化/同步残留），防双倍加成；双端安全。 */
    private static void sanitize(AttributeInstance instance, VanillaBoonAttribute owner) {
        var expected = new HashSet<UUID>();
        for (var mode : BoonModifier.Mode.values()) expected.add(DynamicBoonModifier.modifierId(owner, mode));
        var prefix = "stellaeomphalos.boon." + owner.id().getPath() + ".";
        for (var modifier : instance.getModifiers().stream()
                .filter(m -> m.getName().startsWith(prefix) && !expected.contains(m.getId())).toList())
            instance.removeModifier(modifier);
    }

    private static void clampHealth(Player player) {
        float max = player.getMaxHealth();
        if (player.getHealth() > max) player.setHealth(max);
    }

    private static void onInvalidate(UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var player = server.getPlayerList().getPlayer(playerId);
        if (player != null) refreshAll(player);
    }
}
