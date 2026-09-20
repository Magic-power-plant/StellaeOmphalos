package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** GUI 查询门面；实例级读取器覆盖用于"预览未点亮星眷"等场景。构建入口收紧为静态工厂。 */
public final class BoonStatInterpreter {

    private final Player player;
    private final Map<ResourceLocation, BoonStatReader> overrides;

    private BoonStatInterpreter(Player player, Map<ResourceLocation, BoonStatReader> overrides) {
        this.player = player;
        this.overrides = overrides;
    }

    public static Builder builder(Player player) {
        return new Builder(player);
    }

    public BoonStatLine stat(BoonAttribute attribute) {
        var reader = overrides.getOrDefault(attribute.id(), BoonStatReaderRegistry.readerFor(attribute));
        return reader.display(player);
    }

    public List<BoonStatLine> stats() {
        return BoonAttributeRegistry.all().stream().map(this::stat).toList();
    }

    public static final class Builder {
        private final Player player;
        private final Map<ResourceLocation, BoonStatReader> overrides = new LinkedHashMap<>();

        private Builder(Player player) {
            if (player == null) throw new IllegalArgumentException("null player");
            this.player = player;
        }

        public Builder override(BoonStatReader reader) {
            overrides.put(reader.attribute().id(), reader);
            return this;
        }

        public BoonStatInterpreter build() {
            return new BoonStatInterpreter(player, Map.copyOf(overrides));
        }
    }
}
