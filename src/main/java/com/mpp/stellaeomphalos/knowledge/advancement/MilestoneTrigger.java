package com.mpp.stellaeomphalos.knowledge.advancement;

import com.google.gson.JsonObject;

import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

/** Forge 1.20.1 trigger API; the platform register is CriteriaTriggers, not a deferred registry. */
public abstract class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.Instance> {
    private final ResourceLocation id;

    protected MilestoneTrigger(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public final ResourceLocation getId() {
        return id;
    }

    @Override
    protected final Instance createInstance(
            JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(id, player, MilestoneCriteria.parse(json), json.deepCopy());
    }

    public final void fire(
            ServerPlayer player,
            ResourceLocation subject,
            String category,
            int amount,
            String tier) {
        fireMatching(player, criteria -> criteria.matches(subject, category, amount, tier));
    }

    public final void fireMatching(
            ServerPlayer player, java.util.function.Predicate<MilestoneCriteria> predicate) {
        if (!(player instanceof FakePlayer))
            trigger(player, instance -> predicate.test(instance.conditions));
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final MilestoneCriteria conditions;
        private final JsonObject original;

        private Instance(
                ResourceLocation id,
                ContextAwarePredicate player,
                MilestoneCriteria conditions,
                JsonObject original) {
            super(id, player);
            this.conditions = conditions;
            this.original = original;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            var json = super.serializeToJson(context);
            original.entrySet()
                    .forEach(
                            e -> {
                                if (!e.getKey().equals("player"))
                                    json.add(e.getKey(), e.getValue().deepCopy());
                            });
            return json;
        }
    }
}
