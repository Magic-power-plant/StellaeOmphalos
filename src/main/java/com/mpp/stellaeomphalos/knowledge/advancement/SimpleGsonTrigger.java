package com.mpp.stellaeomphalos.knowledge.advancement;

import com.google.gson.JsonObject;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 《方块物品实体完整清单》进度触发器的公共骨架：沿用原版 1.20.1 的 GSON 反序列化路径
 * （{@code SimpleCriterionTrigger#createInstance}），并把原版解析出的
 * {@link ContextAwarePredicate} 原样交给子类构造条件实例（避免丢失玩家的额外条件）。
 *
 * <p>每个条件实例另外持有一个 {@code CODEC} 字段：1.20.1 原版触发器只走 GSON，
 * 该 Codec 供外部（数据生成 / 自检）以结构化方式读写同一份条件，不参与原版反序列化。
 */
public abstract class SimpleGsonTrigger<T extends AbstractCriterionTriggerInstance>
        extends SimpleCriterionTrigger<T> {

    /** 条件 JSON 中"主体 id"的字段名：星坛配方 / 星象 / 水晶共用同一字段。 */
    protected static final String SUBJECT_FIELD = "sign";

    private final ResourceLocation id;
    private final BiFunction<JsonObject, ContextAwarePredicate, T> factory;

    protected SimpleGsonTrigger(
            ResourceLocation id, BiFunction<JsonObject, ContextAwarePredicate, T> factory) {
        this.id = id;
        this.factory = factory;
    }

    @Override
    public final ResourceLocation getId() {
        return id;
    }

    @Override
    protected final T createInstance(
            JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return factory.apply(json, player);
    }

    /** 统一的触发入口：跳过空玩家与假玩家，并把条件谓词交给原版监听器匹配。 */
    protected final void fire(ServerPlayer player, Predicate<T> condition) {
        if (player == null || player instanceof FakePlayer) return;
        trigger(player, condition);
    }

    /** 把"可选主体 id"写进条件 JSON；为空时不写字段，与 {@link #readSubject} 的通配语义对称。 */
    protected static void writeSubject(JsonObject json, Optional<ResourceLocation> subject) {
        subject.ifPresent(value -> json.addProperty(SUBJECT_FIELD, value.toString()));
    }

    /** 读取主体 id；缺失或非法 id 视为通配（{@link Optional#empty()}）。 */
    protected static Optional<ResourceLocation> readSubject(JsonObject json) {
        var element = json.get(SUBJECT_FIELD);
        if (element == null || !element.isJsonPrimitive()) return Optional.empty();
        return Optional.ofNullable(ResourceLocation.tryParse(element.getAsString()));
    }
}
