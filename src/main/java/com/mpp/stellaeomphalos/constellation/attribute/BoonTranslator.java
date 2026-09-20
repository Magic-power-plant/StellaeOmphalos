package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public abstract class BoonTranslator {

    public static final BoonTranslator IDENTITY = new BoonTranslator() {
        @Override
        public List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode) {
            return List.of(in);
        }
    };

    /** 星眷树网格坐标查询钩子，由星眷树模块在装配期注入；未注入时半径转译保守放行原修饰符。 */
    public interface NodeGridLookup {
        @Nullable
        int[] gridOf(ResourceLocation nodeId);
    }

    private static volatile NodeGridLookup nodeGridLookup;

    public static void setNodeGridLookup(@Nullable NodeGridLookup lookup) {
        nodeGridLookup = lookup;
    }

    public abstract List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode);

    public List<BoonModifier> extraModifiers(Player player, BoonModifier in) {
        return List.of();
    }

    public final BoonTranslator andThen(BoonTranslator next) {
        Objects.requireNonNull(next, "next");
        var self = this;
        return new BoonTranslator() {
            @Override
            public List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode) {
                var out = new ArrayList<BoonModifier>();
                for (var first : self.translate(player, in, ownerNode))
                    out.addAll(next.translate(player, first, ownerNode));
                return out;
            }

            @Override
            public List<BoonModifier> extraModifiers(Player player, BoonModifier in) {
                return Stream.concat(self.extraModifiers(player, in).stream(), next.extraModifiers(player, in).stream()).toList();
            }
        };
    }

    public final BoonTranslator withinRadius(int gridX, int gridZ, double radius) {
        var self = this;
        return new BoonTranslator() {
            @Override
            public List<BoonModifier> translate(Player player, BoonModifier in, ResourceLocation ownerNode) {
                if (!inside(ownerNode, gridX, gridZ, radius)) return List.of(in);
                return self.translate(player, in, ownerNode);
            }

            @Override
            public List<BoonModifier> extraModifiers(Player player, BoonModifier in) {
                if (!inside(in.ownerNode(), gridX, gridZ, radius)) return List.of();
                return self.extraModifiers(player, in);
            }
        };
    }

    private static boolean inside(@Nullable ResourceLocation node, int gridX, int gridZ, double radius) {
        var lookup = nodeGridLookup;
        if (lookup == null || node == null) return false;
        int[] grid = lookup.gridOf(node);
        if (grid == null || grid.length < 2) return false;
        double dx = grid[0] - gridX;
        double dz = grid[1] - gridZ;
        return dx * dx + dz * dz <= radius * radius;
    }
}
