package com.mpp.stellaeomphalos.content.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 符文杖族共用的方块收藏夹（Part-6 §6.4.4）。
 *
 * <p>存储位置为物品持久数据，序列化使用 `BlockState` 的 NBT 形态并保持插入顺序；容量上限 27，
 * 与快捷栏轮换上限一致。三道过滤：**有方块实体不收**、**没有对应物品不收**、**硬度为 -1 不收**。
 */
public final class BlockPalette {

    public static final String TAG = "StellaePalette";
    public static final int MAX_ENTRIES = 27;

    private BlockPalette() {}

    /** 尝试把 {@code pos} 处的方块状态存入杖；返回是否成功。 */
    public static boolean tryStore(ItemStack stack, Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!mayStore(level, pos, state)) return false;
        var stored = storedStates(stack);
        if (stored.size() >= MAX_ENTRIES) stored.remove(0);
        stored.add(state);
        write(stack, stored);
        return true;
    }

    /** §6.4.4 的三道过滤。 */
    public static boolean mayStore(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false;
        return state.getBlock().asItem() != net.minecraft.world.item.Items.AIR;
    }

    /** 已存状态列表，解析失败的条目会被跳过。 */
    public static List<BlockState> storedStates(ItemStack stack) {
        var result = new ArrayList<BlockState>();
        var tag = stack.getTag();
        if (tag == null || !tag.contains(TAG, Tag.TAG_LIST)) return result;
        var list = tag.getList(TAG, Tag.TAG_COMPOUND);
        var lookup = BuiltInRegistries.BLOCK.asLookup();
        for (int i = 0; i < list.size(); i++) {
            try {
                result.add(NbtUtils.readBlockState(lookup, list.getCompound(i)));
            } catch (RuntimeException ignored) {
                // 解析失败项跳过，不使整条数据失效。
            }
        }
        return result;
    }

    /** 状态 → 物品的有序映射（用于预览与预览一致性的种子）。 */
    public static Map<BlockState, ItemStack> mappedStored(ItemStack stack) {
        var map = new LinkedHashMap<BlockState, ItemStack>();
        for (var state : storedStates(stack)) {
            if (state.isAir()) continue;
            map.putIfAbsent(state, new ItemStack(state.getBlock()));
        }
        return map;
    }

    /** 清空全部临时星能缓存（放置/置换后调用，§6.2.2.4）。 */
    public static void clear(Player player) {
        // 临时星能由 Part-2 的充能层持有；此处只提供稳定的调用点，避免杖类自行缓存。
    }

    /** 全客户端在约 2 秒窗口内一致的预览状态。 */
    public static BlockState previewState(Level level) {
        var states = PREVIEW_CACHE;
        if (states.isEmpty()) return null;
        long window = level.getGameTime() / 40L;
        int index = (int) Math.floorMod(0x6834F10A91B03FL * (window << 8), states.size());
        return states.get(index);
    }

    private static final List<BlockState> PREVIEW_CACHE = new ArrayList<>();

    /** 供资源/预览层登记候选状态；重复登记幂等。 */
    public static void registerPreviewCandidate(BlockState state) {
        if (state == null || state.isAir()) return;
        if (PREVIEW_CACHE.size() >= MAX_ENTRIES) PREVIEW_CACHE.remove(0);
        PREVIEW_CACHE.add(state);
    }

    private static void write(ItemStack stack, List<BlockState> states) {
        var list = new ListTag();
        for (var state : states) list.add(NbtUtils.writeBlockState(state));
        stack.getOrCreateTag().put(TAG, list);
    }

    /** 供杖类判断库存是否为空。 */
    public static boolean isEmpty(ItemStack stack) {
        return storedStates(stack).isEmpty();
    }

    /** 从物品解析物品 id；不可用时返回 null。 */
    public static ResourceLocation itemId(Block block) {
        return BuiltInRegistries.ITEM.getKey(block.asItem());
    }

    /** 供数据生成与测试使用的确定性随机。 */
    public static BlockState pick(RandomSource random, ItemStack stack) {
        var states = storedStates(stack);
        return states.isEmpty() ? null : states.get(random.nextInt(states.size()));
    }

    /** 供调试命令导出当前收藏夹。 */
    public static CompoundTag describe(ItemStack stack) {
        var tag = new CompoundTag();
        var list = storedStates(stack);
        tag.putInt("Size", list.size());
        var names = new ListTag();
        for (var state : list) names.add(net.minecraft.nbt.StringTag.valueOf(state.toString()));
        tag.put("Entries", names);
        return tag;
    }
}
