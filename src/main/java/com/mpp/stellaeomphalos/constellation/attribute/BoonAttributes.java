package com.mpp.stellaeomphalos.constellation.attribute;

import net.minecraft.resources.ResourceLocation;

/** 21 个行为/桥接属性 + boon_potency 倍率载体的常量清单。原版桥接 8 项经访问器延迟取注册表，避免静态加载原版注册表。 */
public final class BoonAttributes {

    private static final String NAMESPACE = "stellaeomphalos";

    public static final BoonAttribute ELEMENTAL_WARD = register("elemental_ward", 0.0, BoonAttributeClamp.of(0.0, 0.60), false);
    public static final BoonAttribute PROJECTILE_VELOCITY = register("projectile_velocity", 1.0, BoonAttributeClamp.of(0.0, 3.0), true);
    public static final BoonAttribute HARVEST_SPEED = register("harvest_speed", 1.0, BoonAttributeClamp.of(0.0, 8.0), true);
    public static final BoonAttribute CRIT_CHANCE = register("crit_chance", 0.0, BoonAttributeClamp.of(0.0, 100.0), false);
    public static final BoonAttribute CRIT_DAMAGE = register("crit_damage", 1.0, BoonAttributeClamp.of(1.0, 6.0), true);
    public static final BoonAttribute DODGE = register("dodge", 0.0, BoonAttributeClamp.of(0.0, 0.75), false);
    public static final BoonAttribute DYNAMIC_ENCHANT = register("dynamic_enchant", 1.0, BoonAttributeClamp.of(0.0, 8.0), true);
    public static final BoonAttribute LIFE_LEECH = register("life_leech", 0.0, BoonAttributeClamp.of(0.0, 0.10), false);
    public static final BoonAttribute LIFE_RECOVERY = register("life_recovery", 1.0, BoonAttributeClamp.of(0.0, 5.0), true);
    public static final BoonAttribute BENEFICIAL_DURATION = register("beneficial_duration", 1.0, BoonAttributeClamp.of(1.0, 4.0), true);
    public static final BoonAttribute PROJECTILE_DAMAGE = register("projectile_damage", 1.0, BoonAttributeClamp.of(0.0, 5.0), true);
    public static final BoonAttribute THORNS = register("thorns", 0.0, BoonAttributeClamp.of(0.0, 1.0), false);
    public static final BoonAttribute THORNS_RANGED = register("thorns_ranged", 0.0, BoonAttributeClamp.of(0.0, 1.0), false);

    public static final BoonAttribute BOON_POTENCY = register("boon_potency", 1.0, BoonAttributeClamp.of(0.0, Double.POSITIVE_INFINITY), false);

    private BoonAttributes() {}

    private static ResourceLocation id(String path) {
        return new ResourceLocation(NAMESPACE, path);
    }

    private static BoonAttribute register(String path, double defaultValue, BoonAttributeClamp clamp, boolean onlyMultiplicative) {
        return BoonAttributeRegistry.register(new BoonAttribute(id(path), defaultValue, clamp, onlyMultiplicative));
    }

    public static BoonAttribute armor() {
        return BoonAttributeRegistry.require(id("armor"));
    }

    public static BoonAttribute armorToughness() {
        return BoonAttributeRegistry.require(id("armor_toughness"));
    }

    public static BoonAttribute attackSpeed() {
        return BoonAttributeRegistry.require(id("attack_speed"));
    }

    public static BoonAttribute maxHealth() {
        return BoonAttributeRegistry.require(id("max_health"));
    }

    public static BoonAttribute reach() {
        return BoonAttributeRegistry.require(id("reach"));
    }

    public static BoonAttribute meleeDamage() {
        return BoonAttributeRegistry.require(id("melee_damage"));
    }

    public static BoonAttribute movementSpeed() {
        return BoonAttributeRegistry.require(id("movement_speed"));
    }

    public static BoonAttribute swimSpeed() {
        return BoonAttributeRegistry.require(id("swim_speed"));
    }

    public static void init() {}
}
