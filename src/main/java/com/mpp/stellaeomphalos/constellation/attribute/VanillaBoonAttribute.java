package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

/** 原版桥接属性：星眷通道值经 DynamicBoonModifier 实时映射到原版 Attribute。 */
public abstract class VanillaBoonAttribute extends BoonAttribute {

    private static final String NAMESPACE = "stellaeomphalos";

    private final List<Supplier<Attribute>> targets;

    protected VanillaBoonAttribute(String path, double defaultValue, List<Supplier<Attribute>> targets) {
        super(new ResourceLocation(NAMESPACE, path), defaultValue, BoonAttributeClamp.UNBOUNDED, false);
        this.targets = targets;
    }

    public final List<Attribute> vanillaAttributes() {
        return targets.stream().map(Supplier::get).toList();
    }

    public final DynamicBoonModifier createModifier(Player player, BoonModifier.Mode mode) {
        return new DynamicBoonModifier(player, this, mode);
    }

    public static final class Armor extends VanillaBoonAttribute {
        public Armor() {
            super("armor", 0.0, List.of(() -> Attributes.ARMOR));
        }
    }

    public static final class ArmorToughness extends VanillaBoonAttribute {
        public ArmorToughness() {
            super("armor_toughness", 0.0, List.of(() -> Attributes.ARMOR_TOUGHNESS));
        }
    }

    public static final class AttackSpeed extends VanillaBoonAttribute {
        public AttackSpeed() {
            super("attack_speed", 0.0, List.of(() -> Attributes.ATTACK_SPEED));
        }
    }

    public static final class MaxHealth extends VanillaBoonAttribute {
        public MaxHealth() {
            super("max_health", 0.0, List.of(() -> Attributes.MAX_HEALTH));
        }
    }

    public static final class Reach extends VanillaBoonAttribute {
        public Reach() {
            super("reach", 0.0, List.of(ForgeMod.BLOCK_REACH, ForgeMod.ENTITY_REACH));
        }
    }

    public static final class MeleeDamage extends VanillaBoonAttribute {
        public MeleeDamage() {
            super("melee_damage", 0.0, List.of(() -> Attributes.ATTACK_DAMAGE));
        }
    }

    public static final class MovementSpeed extends VanillaBoonAttribute {
        public MovementSpeed() {
            super("movement_speed", 0.0, List.of(() -> Attributes.MOVEMENT_SPEED));
        }
    }

    public static final class SwimSpeed extends VanillaBoonAttribute {
        public SwimSpeed() {
            super("swim_speed", 0.0, List.of(ForgeMod.SWIM_SPEED));
        }
    }

    public static List<VanillaBoonAttribute> registerAll() {
        var all = List.of(new Armor(), new ArmorToughness(), new AttackSpeed(), new MaxHealth(),
                new Reach(), new MeleeDamage(), new MovementSpeed(), new SwimSpeed());
        all.forEach(BoonAttributeRegistry::register);
        return all;
    }
}
