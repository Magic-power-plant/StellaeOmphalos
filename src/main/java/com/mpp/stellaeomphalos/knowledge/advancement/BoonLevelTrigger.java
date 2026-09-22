package com.mpp.stellaeomphalos.knowledge.advancement;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.platform.BoonLevelQuery;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 进度触发器 {@code boon_level}：玩家星眷（boon）等级达到阈值时触发。
 *
 * <p>与其他四个触发器不同，本触发器是**自查询式谓词**：条件只保存 {@code min_level}，
 * 触发时用 {@link BoonLevelQuery#level} 现读玩家等级，因此调用方无需传入等级。
 */
public final class BoonLevelTrigger extends SimpleGsonTrigger<BoonLevelTrigger.Instance> {

    /** 条件 JSON 中的等级字段名。 */
    public static final String MIN_LEVEL_FIELD = "min_level";

    public BoonLevelTrigger() {
        super(new ResourceLocation(Omphalos.MODID, "boon_level"), BoonLevelTrigger::instance);
    }

    /** 供星眷升级流程调用；等级条件由实例自行读取玩家数据判断。 */
    public void trigger(ServerPlayer player) {
        fire(player, instance -> instance.matches(player));
    }

    private static Instance instance(JsonObject json, ContextAwarePredicate player) {
        var element = json.get(MIN_LEVEL_FIELD);
        int minLevel = element != null && element.isJsonPrimitive() ? element.getAsInt() : 0;
        return new Instance(player, minLevel, json.deepCopy());
    }

    /** 条件实例：保存最低等级并自查询玩家。 */
    public static final class Instance extends AbstractCriterionTriggerInstance {

        private final ContextAwarePredicate player;
        private final int minLevel;
        private final JsonObject original;

        private Instance(ContextAwarePredicate player, int minLevel, JsonObject original) {
            super(new ResourceLocation(Omphalos.MODID, "boon_level"), player);
            this.player = player;
            this.minLevel = Math.max(0, minLevel);
            this.original = original;
        }

        /** 要求的最低星眷等级。 */
        public int minLevel() {
            return minLevel;
        }

        /** 自查询谓词：现读玩家等级并与阈值比较。 */
        public boolean matches(ServerPlayer player) {
            return BoonLevelQuery.level(player) >= minLevel;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            original.entrySet().forEach(entry -> {
                if (!entry.getKey().equals("player") && !entry.getKey().equals(MIN_LEVEL_FIELD))
                    json.add(entry.getKey(), entry.getValue().deepCopy());
            });
            json.add(MIN_LEVEL_FIELD, new JsonPrimitive(minLevel));
            return json;
        }
    }
}
