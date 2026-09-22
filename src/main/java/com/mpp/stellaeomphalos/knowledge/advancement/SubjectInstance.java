package com.mpp.stellaeomphalos.knowledge.advancement;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.resources.ResourceLocation;

/**
 * 带"可选主体 id"的条件实例基类：星坛配方 / 星象 / 水晶三类触发器共用同一形状。
 *
 * <p>主体 id 为空表示通配（不限制具体配方或星象）。子类的 {@code CODEC} 常量
 * 通过 {@code ContextAwarePredicate.CODEC} + {@code ResourceLocation.CODEC.optionalFieldOf("sign")}
 * 构造，与这里的 GSON 回写路径保持一致。
 */
public abstract class SubjectInstance extends AbstractCriterionTriggerInstance {

    private final ContextAwarePredicate player;
    private final Optional<ResourceLocation> subject;
    private final JsonObject original;

    protected SubjectInstance(
            ResourceLocation id,
            ContextAwarePredicate player,
            Optional<ResourceLocation> subject,
            JsonObject original) {
        super(id, player);
        this.player = player;
        this.subject = subject;
        this.original = original;
    }

    /** 该实例要求的主体 id；{@link Optional#empty()} 表示通配。 */
    public final Optional<ResourceLocation> subject() {
        return subject;
    }

    /** 匹配：通配条件接受任意主体，否则要求完全相等。 */
    public final boolean matches(ResourceLocation candidate) {
        return subject.isEmpty() || subject.get().equals(candidate);
    }

    @Override
    public JsonObject serializeToJson(SerializationContext context) {
        JsonObject json = super.serializeToJson(context);
        original.entrySet().forEach(entry -> {
            if (!entry.getKey().equals("player")) json.add(entry.getKey(), entry.getValue().deepCopy());
        });
        SimpleGsonTrigger.writeSubject(json, subject);
        return json;
    }

    /** 供子类 {@code CODEC} 的 {@code forGetter} 引用；同时保留原版玩家谓词语义。 */
    protected ContextAwarePredicate playerPredicate() {
        return player;
    }
}
