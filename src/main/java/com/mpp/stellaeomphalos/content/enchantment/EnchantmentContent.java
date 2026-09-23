package com.mpp.stellaeomphalos.content.enchantment;

import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModEnchantments;
import java.util.ArrayList;
import java.util.List;

/**
 * 《方块物品实体完整清单》附魔的声明与装备 tick 收集器。
 *
 * <p>{@link #initialize()} 必须在 DeferredRegister 登记窗口内调用（由
 * {@code ContentEffects#attach} 触发）。实现刻意**在声明前先构造实例**，再把同一个实例交给
 * {@code DeferredRegister} 与装备 tick 列表：{@code RegistrationGuard#get()} 在登记窗口内会
 * 主动抛出"早于注册表访问"异常，因此这里不能通过守卫回读实例（与 {@code DomainContent} /
 * {@code StarmapContent} 的约定一致：声明期不做任何注册表读取）。
 */
public final class EnchantmentContent {

    private static final List<EquippedTickEnchantment> EQUIPPED_TICK = new ArrayList<>();

    /** 只读视图，避免每 tick 调用方重复拷贝列表。 */
    private static final List<EquippedTickEnchantment> EQUIPPED_TICK_VIEW =
            java.util.Collections.unmodifiableList(EQUIPPED_TICK);

    private static boolean initialized;

    /** id {@code night_vision}：装备时维持夜视。 */
    public static RegistrationGuard<NightVisionEnchantment> NIGHT_VISION;

    /** id {@code scorching_heat}：装备时点燃攻击者并灼烧周围敌对生物。 */
    public static RegistrationGuard<ScorchingHeatEnchantment> SCORCHING_HEAT;

    private EnchantmentContent() {}

    /** 幂等初始化：声明两个附魔实例并把它们登记进装备 tick 列表。 */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        NightVisionEnchantment nightVision = new NightVisionEnchantment();
        ScorchingHeatEnchantment scorchingHeat = new ScorchingHeatEnchantment();
        NIGHT_VISION = ModEnchantments.ENTRIES.declare("night_vision", () -> nightVision);
        SCORCHING_HEAT = ModEnchantments.ENTRIES.declare("scorching_heat", () -> scorchingHeat);
        EQUIPPED_TICK.add(nightVision);
        EQUIPPED_TICK.add(scorchingHeat);
    }

    /**
     * 装配期收集到的"装备时每 tick 生效"附魔；列表内容在 {@link #initialize()} 后不再变化。
     *
     * @return 只读视图，元素顺序与声明顺序一致
     */
    public static List<EquippedTickEnchantment> equippedTickEnchantments() {
        return EQUIPPED_TICK_VIEW;
    }
}
