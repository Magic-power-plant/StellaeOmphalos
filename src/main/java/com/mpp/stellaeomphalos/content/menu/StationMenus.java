package com.mpp.stellaeomphalos.content.menu;

import com.mpp.stellaeomphalos.content.block.MachineContent;
import com.mpp.stellaeomphalos.content.block.MachineContent.MachineBlockEntity;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.List;
import java.util.function.Supplier;

/**
 * 《方块物品实体完整清单》§6.2.9 的容器族：观星镜朝向容器、星图台容器、观星台容器。
 *
 * <p>三者都是**服务端权威**的容器：`stillValid` 每次都复核方块仍在原位、仍是同一方块且玩家在
 * {@value #REACH} 格内，避免玩家把机器拆走或走远之后继续操作。客户端 Screen 归《客户端渲染界面与音效》，
 * 观星镜的朝向更新包归《服务端机制数据存储与网络协议》。
 *
 * <p>容器在两端都由同一个 {@code IForgeMenuType} 工厂从位置构造；方块实体一律经
 * {@code level.getBlockEntity(pos)} 解析（客户端也有同步的 BE 实例），因此不需要额外的同步通道。
 */
public final class StationMenus {

    /** 容器作用距离的平方。 */
    public static final double REACH = 64.0D;

    public static final RegistrationGuard<MenuType<SpyglassMenu>> SPYGLASS =
            ModMenus.ENTRIES.declare(
                    "spyglass",
                    () ->
                            IForgeMenuType.create(
                                    (id, inv, buf) ->
                                            new SpyglassMenu(id, inv, buf.readBlockPos())));

    public static final RegistrationGuard<MenuType<StarChartTableMenu>> STAR_CHART_TABLE =
            ModMenus.ENTRIES.declare(
                    "star_chart_table",
                    () ->
                            IForgeMenuType.create(
                                    (id, inv, buf) ->
                                            new StarChartTableMenu(id, inv, buf.readBlockPos())));

    public static final RegistrationGuard<MenuType<ObservatoryMenu>> OBSERVATORY =
            ModMenus.ENTRIES.declare(
                    "observatory",
                    () ->
                            IForgeMenuType.create(
                                    (id, inv, buf) ->
                                            new ObservatoryMenu(id, inv, buf.readBlockPos())));

    private StationMenus() {}

    public static void initialize() {}

    /** 本册交付的容器 id（供测试与文档引用）。 */
    public static List<String> ids() {
        return List.of("spyglass", "star_chart_table", "observatory");
    }

    /** 打开容器时写入位置，两端一致。 */
    public static void writePos(FriendlyByteBuf buffer, BlockPos pos) {
        buffer.writeBlockPos(pos);
    }

    /** 距离与方块一致性复核；所有《方块物品实体完整清单》容器共用。 */
    public static boolean stillValid(Player player, BlockPos pos, Supplier<Block> expected) {
        if (player == null) return false;
        var block = expected.get();
        if (block == null) return false;
        if (!player.level().getBlockState(pos).is(block)) return false;
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)
                <= REACH;
    }

    /** 无槽位容器基类：只有位置与有效性判定，但仍需放置玩家物品栏槽位。 */
    public abstract static class StationMenu extends AbstractContainerMenu {
        private final BlockPos pos;

        protected StationMenu(MenuType<?> type, int id, BlockPos pos) {
            super(type, id);
            this.pos = pos.immutable();
        }

        public BlockPos pos() {
            return pos;
        }

        protected final void addPlayerInventory(Inventory inventory) {
            for (int row = 0; row < 3; row++)
                for (int column = 0; column < 9; column++)
                    addSlot(
                            new Slot(
                                    inventory,
                                    column + row * 9 + 9,
                                    8 + column * 18,
                                    84 + row * 18));
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    /** `spyglass`：观星镜朝向容器（朝向更新经《服务端机制数据存储与网络协议》的包写入 BE）。 */
    public static final class SpyglassMenu extends StationMenu {
        public SpyglassMenu(int id, Inventory inventory, BlockPos pos) {
            super(SPYGLASS.get(), id, pos);
            addPlayerInventory(inventory);
        }

        @Override
        public boolean stillValid(Player player) {
            return StationMenus.stillValid(player, pos(), () -> MachineContent.SPYGLASS.get());
        }

        @Override public boolean clickMenuButton(Player player, int action) {
            if (player.level().isClientSide || !stillValid(player) || (action != 0 && action != 1)) return false;
            var machine = entity(player.level(), pos());
            if (machine == null) return false;
            machine.setRotation(machine.rotation() + (action == 0 ? -1 : 1));
            return true;
        }


        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }
    }

    /** `observatory`：观星台容器，用于判定玩家是否停在自由观察位。 */
    public static final class ObservatoryMenu extends StationMenu {
        public ObservatoryMenu(int id, Inventory inventory, BlockPos pos) {
            super(OBSERVATORY.get(), id, pos);
            addPlayerInventory(inventory);
        }

        @Override
        public boolean stillValid(Player player) {
            return StationMenus.stillValid(
                    player,
                    pos(),
                    () ->
                            com.mpp.stellaeomphalos.content.world.WorldContent.BLOCKS
                                    .get("observatory")
                                    .get());
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }
    }

    /** `star_chart_table`：星图台容器（输入槽 + 星图玻璃槽 + 绘制进度数据槽）。 */
    public static final class StarChartTableMenu extends StationMenu {

        /** 输入槽索引。 */
        public static final int SLOT_INPUT = 0;

        /** 星图玻璃槽索引。 */
        public static final int SLOT_GLASS = 1;

        /** 机器槽位数量。 */
        public static final int MACHINE_SLOTS = 2;

        /** 数据槽数量：绘制进度 + 羊皮纸数。 */
        public static final int DATA_SIZE = 2;

        private final Inventory inventory;
        private final net.minecraft.world.inventory.SimpleContainerData synchronizedData =
                new net.minecraft.world.inventory.SimpleContainerData(DATA_SIZE);

        public StarChartTableMenu(int id, Inventory inventory, BlockPos pos) {
            super(STAR_CHART_TABLE.get(), id, pos);
            this.inventory = inventory;
            addSlot(new SlotItemHandler(handler(inventory, pos), SLOT_INPUT, 44, 36));
            addSlot(new SlotItemHandler(handler(inventory, pos), SLOT_GLASS, 116, 36));
            addPlayerInventory(inventory);
            addDataSlots(inventory.player.level().isClientSide ? synchronizedData : data(inventory, pos));
        }

        public int runTick() {
            if (inventory.player.level().isClientSide) return synchronizedData.get(0);
            var entity = entity(inventory.player.level(), pos());
            return entity == null ? 0 : entity.runTick();
        }

        public int parchment() {
            if (inventory.player.level().isClientSide) return synchronizedData.get(1);
            var entity = entity(inventory.player.level(), pos());
            return entity == null ? 0 : entity.parchment();
        }

        @Override
        public boolean stillValid(Player player) {
            return StationMenus.stillValid(
                    player, pos(), () -> MachineContent.STAR_CHART_TABLE.get());
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
            var slot = slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;
            var stack = slot.getItem();
            var copy = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true))
                    return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, SLOT_INPUT, SLOT_GLASS + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            return copy;
        }
    }

    /** 从玩家所在世界解析星图台方块实体；两端均可用（客户端也有同步 BE）。 */
    static MachineBlockEntity entity(Level level, BlockPos pos) {
        if (level == null || !level.hasChunkAt(pos)) return null;
        return level.getBlockEntity(pos) instanceof MachineBlockEntity machine ? machine : null;
    }

    /** 槽位句柄：直接读写方块实体的物品栏；BE 缺失时回落到空实现（不会凭空生成物品）。 */
    private static ItemStackHandler handler(Inventory inventory, BlockPos pos) {
        return new ItemStackHandler(StarChartTableMenu.MACHINE_SLOTS) {
            private MachineBlockEntity entity() {
                return StationMenus.entity(inventory.player.level(), pos);
            }

            @Override
            public int getSlots() {
                var entity = entity();
                return entity == null ? super.getSlots() : entity.inventory().getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                var entity = entity();
                return entity == null
                        ? super.getStackInSlot(slot)
                        : entity.inventory().getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                var entity = entity();
                return entity == null
                        ? super.insertItem(slot, stack, simulate)
                        : entity.inventory().insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                var entity = entity();
                return entity == null
                        ? super.extractItem(slot, amount, simulate)
                        : entity.inventory().extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                var entity = entity();
                return entity == null
                        ? super.getSlotLimit(slot)
                        : entity.inventory().getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                var entity = entity();
                return entity == null || entity.inventory().isItemValid(slot, stack);
            }
        };
    }

    /** 进度数据槽：两端都直接读同一方块实体（客户端 BE 由原版同步）。 */
    private static ContainerData data(Inventory inventory, BlockPos pos) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                var entity = StationMenus.entity(inventory.player.level(), pos);
                if (entity == null) return 0;
                return index == 0 ? entity.runTick() : entity.parchment();
            }

            @Override
            public void set(int index, int value) {
                // 进度只能由方块实体自身推进，容器不接受外部写入。
            }

            @Override
            public int getCount() {
                return StarChartTableMenu.DATA_SIZE;
            }
        };
    }
}
