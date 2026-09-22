package com.mpp.stellaeomphalos.knowledge.advancement;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.Omphalos;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 进度触发器 {@code altar_craft}：完成指定星坛配方时触发。
 * 条件中的 {@code sign} 字段保存配方 id；缺省表示"任意配方"。
 */
public final class AltarCraftTrigger extends SimpleGsonTrigger<AltarCraftTrigger.Instance> {

    public AltarCraftTrigger() {
        super(new ResourceLocation(Omphalos.MODID, "altar_craft"),
                (json, player) -> new Instance(player, readSubject(json), json.deepCopy()));
    }

    /** 供星坛合成完成处调用。 */
    public void trigger(ServerPlayer player, ResourceLocation recipe) {
        fire(player, instance -> instance.matches(recipe));
    }

    /** 条件实例：可选配方 id 通配匹配。 */
    public static final class Instance extends SubjectInstance {

        private Instance(ContextAwarePredicate player, Optional<ResourceLocation> recipe, JsonObject original) {
            super(new ResourceLocation(Omphalos.MODID, "altar_craft"), player, recipe, original);
        }

        /** 该实例要求的配方 id；空表示通配。 */
        public Optional<ResourceLocation> recipe() {
            return subject();
        }
    }
}
