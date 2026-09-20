package com.mpp.stellaeomphalos.lumen.transport.stasis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** Who a stasis zone freezes. Block entities have no owner notion and are always frozen inside a zone. */
public record StasisFilter(Mode mode, @Nullable UUID owner, boolean targetPlayers) {
    public enum Mode {
        ALL_EXCEPT, NO_PLAYERS;
        public String serialized() { return name().toLowerCase(Locale.ROOT); }
        public static Optional<Mode> bySerialized(String text) {
            for (var mode : values()) if (mode.serialized().equals(text)) return Optional.of(mode);
            return Optional.empty();
        }
        public static final Codec<Mode> CODEC = Codec.STRING.comapFlatMap(text -> bySerialized(text)
                .map(DataResult::success).orElseGet(() -> DataResult.error(() -> "Unknown stasis filter mode " + text)), Mode::serialized);
    }

    public static final Codec<StasisFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Mode.CODEC.fieldOf("Mode").forGetter(StasisFilter::mode),
            UUIDUtil.STRING_CODEC.optionalFieldOf("Owner").forGetter(filter -> Optional.ofNullable(filter.owner())),
            Codec.BOOL.fieldOf("TargetPlayers").forGetter(StasisFilter::targetPlayers)
    ).apply(instance, (mode, owner, targetPlayers) -> new StasisFilter(mode, owner.orElse(null), targetPlayers)));

    public boolean freezes(Entity entity) {
        return decide(mode, owner, targetPlayers, entity instanceof Player, entity.getUUID());
    }

    public boolean freezes(BlockEntity blockEntity) { return true; }

    /** Pure decision core (kept Minecraft-free so JUnit can cover the full truth table). */
    public static boolean decide(Mode mode, @Nullable UUID owner, boolean targetPlayers, boolean isPlayer, UUID entityId) {
        if (!isPlayer) return true;
        if (mode == Mode.NO_PLAYERS || !targetPlayers) return false;
        return owner == null || !owner.equals(entityId);
    }
}
