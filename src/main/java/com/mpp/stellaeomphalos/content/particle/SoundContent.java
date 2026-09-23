package com.mpp.stellaeomphalos.content.particle;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * 《方块物品实体完整清单》音效事件的声明类。全部走可变距离（{@code createVariableRangeEvent}），
 * 分类与字幕由 {@code sounds.json} 决定，Java 侧只登记事件本身。
 *
 * <p>{@code craft_finish} 已在 {@code CraftingContent.CRAFT_FINISH}（id {@code craft_finish}）中登记，
 * 本类**刻意跳过**该 id，避免 DeferredRegister 重复键异常。
 */
public final class SoundContent {

    /** 棱镜换色、透镜插拔。 */
    public static final RegistrationGuard<SoundEvent> CLIP_SWITCH = declare("clip_switch");

    /** 星坛合成循环音、共鸣仪式。 */
    public static final RegistrationGuard<SoundEvent> ATTUNEMENT = declare("attunement");

    /** 星典关闭。 */
    public static final RegistrationGuard<SoundEvent> BOOK_CLOSE = declare("book_close");

    /** 星典翻页、星图台投纸。 */
    public static final RegistrationGuard<SoundEvent> BOOK_FLIP = declare("book_flip");

    private SoundContent() {}

    /** 幂等初始化：强制类加载，使上面的 DeferredRegister 声明落在登记窗口内。 */
    public static void initialize() {
        // 类初始化即完成声明；此方法只作为装配点的显式调用入口。
    }

    /**
     * 本类登记的 4 个音效事件（不含已由 {@code CraftingContent} 登记的 {@code craft_finish}）。
     */
    public static java.util.List<RegistrationGuard<SoundEvent>> all() {
        return java.util.List.of(CLIP_SWITCH, ATTUNEMENT, BOOK_CLOSE, BOOK_FLIP);
    }

    private static RegistrationGuard<SoundEvent> declare(String id) {
        return ModSounds.ENTRIES.declare(
                id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Omphalos.MODID, id)));
    }
}
