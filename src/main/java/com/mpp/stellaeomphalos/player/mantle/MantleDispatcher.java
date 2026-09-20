package com.mpp.stellaeomphalos.player.mantle;

import com.mpp.stellaeomphalos.core.bootstrap.RuntimeServices;
import com.mpp.stellaeomphalos.core.platform.WorldCraftingBridge;
import com.mpp.stellaeomphalos.core.util.world.BulkBlockBreaker;
import com.mpp.stellaeomphalos.lumen.transport.stasis.*;
import com.mpp.stellaeomphalos.network.*;
import com.mpp.stellaeomphalos.network.toClient.PktMantleState;

import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.*;

/**
 * Passive callbacks are centralized; every recursive world action is guarded and permission
 * checked.
 */
public final class MantleDispatcher {
    private record Session(ItemStack stack, MantleEffect effect, MantleRegistry.Kind kind) {}

    private static final ThreadLocal<Boolean> SECONDARY = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, CompoundTag> LAST_SYNC = new HashMap<>();

    private MantleDispatcher() {}

    public static void attach() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(MantleDispatcher::tick);
        bus.addListener(EventPriority.HIGH, MantleDispatcher::hurt);
        bus.addListener(EventPriority.LOWEST, MantleDispatcher::block);
        bus.addListener(MantleDispatcher::death);
    }

    public static void clear() {
        LAST_SYNC.clear();
    }

    public static void logout(UUID id) {
        LAST_SYNC.remove(id);
    }

    private static Optional<Session> session(ServerPlayer player) {
        if (player instanceof FakePlayer) return Optional.empty();
        var stack = MantleSlotBridge.equipped(player);
        if (stack.isEmpty()
                || !net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .equals(new ResourceLocation("stellaeomphalos:star_mantle")))
            return Optional.empty();
        var tag = stack.hasTag() ? stack.getTag() : new CompoundTag();
        var sign = ResourceLocation.tryParse(tag.getString("SignId"));
        if (sign == null) return Optional.empty();
        return MantleRegistry.forSign(sign)
                .map(
                        kind -> {
                            var id = MantleRegistry.id(kind);
                            var parsed =
                                    MantleState.CODEC
                                            .parse(NbtOps.INSTANCE, tag.getCompound("MantleState"))
                                            .result();
                            var state =
                                    parsed.filter(s -> s.effect().equals(id))
                                            .orElseGet(
                                                    () -> new MantleState(id, new CompoundTag()));
                            return new Session(
                                    stack,
                                    MantleRegistry.create(state, MantleParameters.current()),
                                    kind);
                        });
    }

    private static void save(Session session) {
        session.stack()
                .getOrCreateTag()
                .put(
                        "MantleState",
                        MantleState.CODEC
                                .encodeStart(NbtOps.INSTANCE, session.effect().snapshot())
                                .getOrThrow(false, message -> {}));
    }

    private static MantleAction action(
            ServerPlayer player, MantleAction.Kind kind, float damage, boolean fire) {
        return new MantleAction(
                kind, player.serverLevel().getGameTime(), damage, fire, player.isInWater());
    }

    private static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
            return;
        session(player)
                .ifPresent(
                        session -> {
                            var result =
                                    session.effect()
                                            .perform(
                                                    action(
                                                            player,
                                                            MantleAction.Kind.TICK,
                                                            0,
                                                            false),
                                                    false);
                            if (result.healing() > 0) player.heal(result.healing());
                            var level = player.serverLevel();
                            long now = level.getGameTime();
                            if (session.kind() == MantleRegistry.Kind.GROWTH && now % 100 == 0) {
                                for (var target :
                                        level.getEntitiesOfClass(
                                                net.minecraft.world.entity.player.Player.class,
                                                player.getBoundingBox().inflate(6),
                                                p -> p.isAlive())) {
                                    target.heal((float) MantleParameters.current().healing() * 20);
                                    target.getFoodData().eat(1, .1F);
                                }
                                var pos =
                                        player.blockPosition()
                                                .offset(
                                                        level.random.nextInt(7) - 3,
                                                        -1,
                                                        level.random.nextInt(7) - 3);
                                if (level.hasChunkAt(pos) && level.mayInteract(player, pos)) {
                                    var state = level.getBlockState(pos.above());
                                    if (state.getBlock() instanceof BonemealableBlock plant
                                            && plant.isValidBonemealTarget(
                                                    level, pos.above(), state, false)
                                            && plant.isBonemealSuccess(
                                                    level, level.random, pos.above(), state))
                                        plant.performBonemeal(
                                                level, level.random, pos.above(), state);
                                }
                            }
                            if (session.kind() == MantleRegistry.Kind.HERDER && now % 100 == 0)
                                followers(player, session, true, null);
                            if (session.kind() == MantleRegistry.Kind.HEARTH
                                    && player.isOnFire()
                                    && now % 20 == 0
                                    && level.random.nextFloat() < .1F) {
                                var pos = player.blockPosition().below();
                                var state = level.getBlockState(pos);
                                if (level.mayInteract(player, pos))
                                    WorldCraftingBridge.melting(level, state)
                                            .ifPresent(m -> m.apply(level, pos, state));
                            }
                            if (session.kind() == MantleRegistry.Kind.ARTIFICER
                                    && now > session.effect().state.getLong("TaskUntil"))
                                session.effect().state.putInt("ActiveTasks", 0);
                            save(session);
                            if (now % 5 == 0) {
                                var data = session.stack().getTag().getCompound("MantleState");
                                if (!data.equals(LAST_SYNC.get(player.getUUID()))) {
                                    LAST_SYNC.put(player.getUUID(), data.copy());
                                    var packet =
                                            new PktMantleState(player.getUUID().toString(), data);
                                    for (var viewer : level.players())
                                        if (viewer.distanceToSqr(player) <= 1024)
                                            OmphalosChannel.send(viewer, packet);
                                }
                            }
                        });
    }

    private static void hurt(LivingHurtEvent event) {
        if (SECONDARY.get()) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker)
            session(attacker)
                    .ifPresent(
                            s -> {
                                event.setAmount(
                                        s.effect()
                                                .perform(
                                                        action(
                                                                attacker,
                                                                MantleAction.Kind.ATTACK,
                                                                event.getAmount(),
                                                                false),
                                                        false)
                                                .damage());
                                save(s);
                                if (s.kind() == MantleRegistry.Kind.ARTIFICER
                                        && attacker.getRandom().nextDouble()
                                                < MantleParameters.current().chance())
                                    toolAttack(attacker, s, event.getEntity());
                            });
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        session(player)
                .ifPresent(
                        s -> {
                            var a =
                                    action(
                                            player,
                                            MantleAction.Kind.HURT,
                                            event.getAmount(),
                                            event.getSource().is(DamageTypeTags.IS_FIRE));
                            var preview = s.effect().perform(a, true);
                            var result = s.effect().perform(a, false);
                            if (preview.immune()) event.setCanceled(true);
                            else event.setAmount(result.damage());
                            if (result.healing() > 0) player.heal(result.healing());
                            if (s.kind() == MantleRegistry.Kind.HERDER
                                    && event.getSource().getEntity() instanceof LivingEntity target)
                                followers(player, s, false, target);
                            if (s.kind() == MantleRegistry.Kind.HOROLOGE
                                    && !player.getCooldowns().isOnCooldown(s.stack().getItem())
                                    && player.getRandom().nextDouble()
                                            < MantleParameters.current().chance()) {
                                if (StasisService.get(player.server)
                                        .activate(
                                                player.serverLevel(),
                                                player.blockPosition(),
                                                5,
                                                new StasisFilter(
                                                        StasisFilter.Mode.ALL_EXCEPT,
                                                        player.getUUID(),
                                                        true),
                                                80,
                                                player.getUUID()))
                                    player.getCooldowns()
                                            .addCooldown(
                                                    s.stack().getItem(),
                                                    MantleParameters.current().cooldown());
                            }
                            save(s);
                        });
    }

    private static void followers(
            ServerPlayer player, Session session, boolean summon, LivingEntity target) {
        var state = session.effect().state;
        var live = new ListTag();
        for (var value : state.getList("Followers", Tag.TAG_STRING))
            try {
                var entity = player.serverLevel().getEntity(UUID.fromString(value.getAsString()));
                if (entity instanceof Wolf wolf
                        && wolf.isAlive()
                        && player.getUUID().equals(wolf.getOwnerUUID())) {
                    live.add(value.copy());
                    if (target != null && !target.isAlliedTo(player)) wolf.setTarget(target);
                }
            } catch (IllegalArgumentException ignored) {
            }
        if (summon && live.size() < 2 && player.serverLevel().getGameTime() % 400 == 0) {
            var wolf = EntityType.WOLF.create(player.serverLevel());
            if (wolf != null) {
                wolf.moveTo(player.getX() + 1, player.getY(), player.getZ(), 0, 0);
                wolf.tame(player);
                if (player.serverLevel().noCollision(wolf)
                        && player.serverLevel().addFreshEntity(wolf))
                    live.add(StringTag.valueOf(wolf.getUUID().toString()));
            }
        }
        state.put("Followers", live);
    }

    private static void block(BlockEvent.BreakEvent event) {
        if (event.isCanceled()
                || SECONDARY.get()
                || !(event.getPlayer() instanceof ServerPlayer player)) return;
        session(player)
                .ifPresent(
                        s -> {
                            if (s.kind() != MantleRegistry.Kind.RUIN
                                    && s.kind() != MantleRegistry.Kind.ARTIFICER) return;
                            if (s.kind() == MantleRegistry.Kind.ARTIFICER
                                    && player.getRandom().nextDouble()
                                            >= MantleParameters.current().chance()) return;
                            var origin = event.getPos().immutable();
                            var dimension = player.serverLevel().dimension();
                            var hit = player.pick(6, 0, false);
                            var axis =
                                    hit instanceof net.minecraft.world.phys.BlockHitResult b
                                            ? b.getDirection().getAxis()
                                            : Direction.Axis.Y;
                            RuntimeServices.current()
                                    .scheduler()
                                    .schedule(
                                            1,
                                            () -> {
                                                if (player.isRemoved()
                                                        || player.serverLevel().dimension()
                                                                != dimension
                                                        || !player.serverLevel()
                                                                .getBlockState(origin)
                                                                .isAir()) return;
                                                secondary(
                                                        () -> {
                                                            int count = 0;
                                                            for (int x = -1; x <= 1; x++)
                                                                for (int y = -1; y <= 1; y++) {
                                                                    if (x == 0 && y == 0
                                                                            || ++count > 8)
                                                                        continue;
                                                                    var p =
                                                                            switch (axis) {
                                                                                case X ->
                                                                                        origin
                                                                                                .offset(
                                                                                                        0,
                                                                                                        x,
                                                                                                        y);
                                                                                case Y ->
                                                                                        origin
                                                                                                .offset(
                                                                                                        x,
                                                                                                        0,
                                                                                                        y);
                                                                                case Z ->
                                                                                        origin
                                                                                                .offset(
                                                                                                        x,
                                                                                                        y,
                                                                                                        0);
                                                                            };
                                                                    if (player.distanceToSqr(
                                                                                            net
                                                                                                    .minecraft
                                                                                                    .world
                                                                                                    .phys
                                                                                                    .Vec3
                                                                                                    .atCenterOf(
                                                                                                            p))
                                                                                    <= 64
                                                                            && !player.serverLevel()
                                                                                    .getBlockState(
                                                                                            p)
                                                                                    .hasBlockEntity()) {
                                                                        if (BulkBlockBreaker
                                                                                .breakOne(
                                                                                        player
                                                                                                .serverLevel(),
                                                                                        p,
                                                                                        player))
                                                                            player.serverLevel()
                                                                                    .sendParticles(
                                                                                            ParticleTypes
                                                                                                    .ENCHANT,
                                                                                            p.getX()
                                                                                                    + .5,
                                                                                            p.getY()
                                                                                                    + .5,
                                                                                            p.getZ()
                                                                                                    + .5,
                                                                                            3,
                                                                                            .2,
                                                                                            .2,
                                                                                            .2,
                                                                                            0);
                                                                    }
                                                                }
                                                        });
                                            });
                        });
    }

    private static void toolAttack(ServerPlayer player, Session session, LivingEntity target) {
        if (session.effect().state.getInt("ActiveTasks") >= 3) return;
        session.effect()
                .state
                .putInt("ActiveTasks", session.effect().state.getInt("ActiveTasks") + 1);
        session.effect().state.putLong("TaskUntil", player.serverLevel().getGameTime() + 40);
        save(session);
        var stack = session.stack();
        RuntimeServices.current()
                .scheduler()
                .schedule(
                        5,
                        () -> {
                            try {
                                if (target.isAlive()
                                        && !player.isRemoved()
                                        && target.level() == player.level()
                                        && target.distanceToSqr(player) < 64)
                                    secondary(
                                            () ->
                                                    target.hurt(
                                                            player.damageSources()
                                                                    .playerAttack(player),
                                                            2));
                            } finally {
                                if (stack.hasTag()) {
                                    var data =
                                            stack.getTag()
                                                    .getCompound("MantleState")
                                                    .getCompound("Data");
                                    data.putInt(
                                            "ActiveTasks",
                                            Math.max(0, data.getInt("ActiveTasks") - 1));
                                }
                            }
                        });
    }

    private static void death(LivingDeathEvent event) {
        if (SECONDARY.get()
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !(event.getEntity() instanceof Monster)) return;
        session(player)
                .filter(s -> s.kind() == MantleRegistry.Kind.RUIN)
                .ifPresent(
                        s ->
                                secondary(
                                        () -> {
                                            for (var mob :
                                                    player.serverLevel()
                                                            .getEntitiesOfClass(
                                                                    Monster.class,
                                                                    event.getEntity()
                                                                            .getBoundingBox()
                                                                            .inflate(4),
                                                                    LivingEntity::isAlive)) {
                                                int invulnerable = mob.invulnerableTime;
                                                try {
                                                    mob.invulnerableTime = 0;
                                                    mob.hurt(
                                                            player.damageSources()
                                                                    .playerAttack(player),
                                                            Math.min(
                                                                    8,
                                                                    event.getEntity().getMaxHealth()
                                                                            * .1F));
                                                } finally {
                                                    mob.invulnerableTime = invulnerable;
                                                }
                                            }
                                        }));
    }

    private static void secondary(Runnable action) {
        boolean previous = SECONDARY.get();
        SECONDARY.set(true);
        try {
            action.run();
        } finally {
            SECONDARY.set(previous);
        }
    }
}
