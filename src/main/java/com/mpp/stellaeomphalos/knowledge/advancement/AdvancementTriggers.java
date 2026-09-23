package com.mpp.stellaeomphalos.knowledge.advancement;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.registry.ModAdvancementTriggers;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 《方块物品实体完整清单》的 5 个进度触发器登记类。
 *
 * <p>{@link #initialize()} 必须在 {@code ModAdvancementTriggers.register()} **之前**调用
 * （装配点在 {@code RegistryBootstrap#setup} 的 {@code FMLCommonSetupEvent} 队列里），
 * 否则会抛出"late trigger"异常。触发器实例是进程级单例，供其它系统通过本类的静态
 * 转发方法或直接调用其 {@code trigger(...)} 入口。
 */
public final class AdvancementTriggers {

    private static final Map<String, CriterionTrigger<?>> TRIGGERS = new LinkedHashMap<>();

    private static boolean initialized;

    private AdvancementTriggers() {}

    /** 幂等初始化：声明 5 个触发器 id 并保留单例引用。 */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        declare("altar_craft", new AltarCraftTrigger());
        declare("attune_self", new AttuneSelfTrigger());
        declare("attune_crystal", new AttuneCrystalTrigger());
        declare("discover_sign", new DiscoverSignTrigger());
        declare("boon_level", new BoonLevelTrigger());
    }

    /** 按短名（不含命名空间）取回触发器单例。 */
    public static Optional<CriterionTrigger<?>> find(String name) {
        return Optional.ofNullable(TRIGGERS.get(name));
    }

    /** 星坛配方完成：由星坛合成流程调用。 */
    public static void altarCraft(ServerPlayer player, ResourceLocation recipe) {
        if (TRIGGERS.get("altar_craft") instanceof AltarCraftTrigger trigger)
            trigger.trigger(player, recipe);
    }

    /** 玩家自身共鸣到指定星象。 */
    public static void attuneSelf(ServerPlayer player, ResourceLocation sign) {
        if (TRIGGERS.get("attune_self") instanceof AttuneSelfTrigger trigger) trigger.trigger(player, sign);
    }

    /** 水晶调谐到指定星象。 */
    public static void attuneCrystal(ServerPlayer player, ResourceLocation sign) {
        if (TRIGGERS.get("attune_crystal") instanceof AttuneCrystalTrigger trigger)
            trigger.trigger(player, sign);
    }

    /** 发现指定星象。 */
    public static void discoverSign(ServerPlayer player, ResourceLocation sign) {
        if (TRIGGERS.get("discover_sign") instanceof DiscoverSignTrigger trigger)
            trigger.trigger(player, sign);
    }

    /** 星眷等级达标（自查询式，无需传入等级）。 */
    public static void boonLevel(ServerPlayer player) {
        if (TRIGGERS.get("boon_level") instanceof BoonLevelTrigger trigger) trigger.trigger(player);
    }

    private static void declare(String name, CriterionTrigger<?> trigger) {
        var id = new ResourceLocation(Omphalos.MODID, name);
        ModAdvancementTriggers.declare(id, () -> trigger);
        TRIGGERS.put(name, trigger);
    }
}
