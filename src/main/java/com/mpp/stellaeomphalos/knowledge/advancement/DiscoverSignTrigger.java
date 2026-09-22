package com.mpp.stellaeomphalos.knowledge.advancement;

import com.google.gson.JsonObject;
import com.mpp.stellaeomphalos.Omphalos;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 进度触发器 {@code discover_sign}：发现（记录进星典）指定星象时触发。
 * 条件中的 {@code sign} 字段保存星象 id；缺省表示"任意星象"。
 */
public final class DiscoverSignTrigger extends SimpleGsonTrigger<DiscoverSignTrigger.Instance> {

    public DiscoverSignTrigger() {
        super(new ResourceLocation(Omphalos.MODID, "discover_sign"),
                (json, player) -> new Instance(player, readSubject(json), json.deepCopy()));
    }

    /** 供星象发现流程调用。 */
    public void trigger(ServerPlayer player, ResourceLocation sign) {
        fire(player, instance -> instance.matches(sign));
    }

    /** 条件实例：可选星象 id 通配匹配。 */
    public static final class Instance extends SubjectInstance {

        private Instance(ContextAwarePredicate player, Optional<ResourceLocation> sign, JsonObject original) {
            super(new ResourceLocation(Omphalos.MODID, "discover_sign"), player, sign, original);
        }

        /** 该实例要求的星象 id；空表示通配。 */
        public Optional<ResourceLocation> sign() {
            return subject();
        }
    }
}
