package com.mpp.stellaeomphalos.constellation.attribute;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/** 原版 AttributeModifier 子类：数值实时向账本索取（经缓存快照）；UUID 由属性 id 确定性派生。
 *  1.20.1 的 AttributeModifier 无 setSaved 标记，靠确定性 UUID 在挂载时替换掉持久化/同步过来的占位副本。 */
public final class DynamicBoonModifier extends AttributeModifier {

    private final WeakReference<Player> player;
    private final VanillaBoonAttribute owner;
    private final BoonModifier.Mode mode;

    public DynamicBoonModifier(Player player, VanillaBoonAttribute owner, BoonModifier.Mode mode) {
        super(modifierId(owner, mode), modifierName(owner, mode), 0.0, mode.toVanilla());
        this.player = new WeakReference<>(player);
        this.owner = owner;
        this.mode = mode;
    }

    public static UUID modifierId(BoonAttribute attribute, BoonModifier.Mode mode) {
        return UUID.nameUUIDFromBytes(("stellaeomphalos:boon/" + attribute.id() + "/" + mode.ordinal())
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String modifierName(BoonAttribute attribute, BoonModifier.Mode mode) {
        return "stellaeomphalos.boon." + attribute.id().getPath() + "." + mode.name().toLowerCase(Locale.ROOT);
    }

    public VanillaBoonAttribute owner() {
        return owner;
    }

    public BoonModifier.Mode boonMode() {
        return mode;
    }

    @Override
    public double getAmount() {
        var resolved = player.get();
        if (resolved == null) return 0.0;
        double aggregate = BoonValueBridge.resolve(resolved, owner, mode);
        return mode == BoonModifier.Mode.STACKING_MULTIPLY ? aggregate - 1.0 : aggregate;
    }
}
