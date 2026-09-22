package com.mpp.stellaeomphalos.content.item.amulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * 护符修正的持久载体（Part-6 §6.3.5 / §6.4.4）。
 *
 * <p>修正列表按 §6.4.4 的第一行放在物品 NBT 的 `Amulet` 子标签下、由 {@link AmuletModifier#CODEC} 序列化；
 * `owner` 同样落在该子标签内（原模组挂在 `ItemStack` 能力上，但本项目暂无 `owner` 消费方，
 * 故不引入一个只写不读的能力——若后续需要按所有者校验，再补 `AttachCapabilitiesEvent<ItemStack>`）。
 */
public final class AmuletHolder {

    /** 物品 NBT 内的子标签键。 */
    public static final String TAG = "Amulet";

    /** 修正列表键。 */
    public static final String MODIFIERS = "Modifiers";

    /** 所有者键。 */
    public static final String OWNER = "Owner";

    /** 掷骰完成标记键。 */
    public static final String ROLLED = "Rolled";

    /** 完整持久结构。 */
    public record Data(List<AmuletModifier> modifiers, Optional<UUID> owner, boolean rolled) {

        public static final Codec<Data> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                AmuletModifier.CODEC
                                                        .listOf()
                                                        .fieldOf("modifiers")
                                                        .forGetter(Data::modifiers),
                                                com.mojang.serialization.Codec.STRING
                                                        .optionalFieldOf("owner")
                                                        .xmap(
                                                                value ->
                                                                        value.flatMap(
                                                                                text -> {
                                                                                    try {
                                                                                        return Optional.of(
                                                                                                UUID.fromString(
                                                                                                        text));
                                                                                    } catch (
                                                                                            IllegalArgumentException
                                                                                                    ignored) {
                                                                                        return Optional.empty();
                                                                                    }
                                                                                }),
                                                                uuid -> uuid.map(UUID::toString))
                                                        .forGetter(Data::owner),
                                                Codec.BOOL
                                                        .optionalFieldOf("rolled", false)
                                                        .forGetter(Data::rolled))
                                        .apply(instance, Data::new));

        public static Data empty() {
            return new Data(List.of(), Optional.empty(), false);
        }
    }

    private AmuletHolder() {}

    /** 读取完整数据；缺失或损坏时返回空结构。 */
    public static Data read(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(TAG, Tag.TAG_COMPOUND)) return Data.empty();
        return Data.CODEC
                .parse(NbtOps.INSTANCE, stack.getTag().getCompound(TAG))
                .result()
                .orElseGet(Data::empty);
    }

    /** 写入完整数据。 */
    public static void write(ItemStack stack, Data data) {
        Data.CODEC
                .encodeStart(NbtOps.INSTANCE, data)
                .result()
                .ifPresent(tag -> stack.getOrCreateTag().put(TAG, tag));
    }

    /** @return 已合并同类项的修正列表；未初始化时为空表 */
    public static List<AmuletModifier> modifiers(ItemStack stack) {
        return read(stack).modifiers();
    }

    /**
     * 惰性初始化：首次装备时冻结颜色并掷出修正（§6.6.2）。
     *
     * @return 是否在本次调用中完成掷骰
     */
    public static boolean rollIfNeeded(
            ItemStack stack,
            RandomSource random,
            List<net.minecraft.resources.ResourceLocation> candidates,
            java.util.function.ToIntFunction<net.minecraft.resources.ResourceLocation> levelOfExisting,
            @Nullable UUID owner) {
        var data = read(stack);
        if (data.rolled()) return false;
        var rolled = AmuletRoller.roll(random, candidates, AmuletRoller.Probabilities.fromConfig(), levelOfExisting);
        write(
                stack,
                new Data(
                        rolled,
                        owner == null ? data.owner() : Optional.of(owner),
                        true));
        return true;
    }

    /** 记录所有者（重掷或转交时调用）。 */
    public static void setOwner(ItemStack stack, @Nullable UUID owner) {
        var data = read(stack);
        write(stack, new Data(data.modifiers(), Optional.ofNullable(owner), data.rolled()));
    }

    /** 供调试/测试：强制写入修正列表。 */
    public static void setModifiers(ItemStack stack, List<AmuletModifier> modifiers) {
        var data = read(stack);
        write(stack, new Data(AmuletRoller.merge(modifiers), data.owner(), true));
    }

    /** 供调试命令使用。 */
    public static CompoundTag describe(ItemStack stack) {
        var data = read(stack);
        var tag = new CompoundTag();
        tag.putInt("Count", data.modifiers().size());
        tag.putBoolean("Rolled", data.rolled());
        data.owner().ifPresent(owner -> tag.putString("Owner", owner.toString()));
        var list = new net.minecraft.nbt.ListTag();
        data.modifiers().forEach(modifier -> list.add(net.minecraft.nbt.StringTag.valueOf(modifier.describe())));
        tag.put("Entries", list);
        return tag;
    }
}
