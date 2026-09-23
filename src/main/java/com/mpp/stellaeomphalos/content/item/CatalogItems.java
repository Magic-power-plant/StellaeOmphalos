package com.mpp.stellaeomphalos.content.item;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.content.item.RodItems.BuilderRodItem;
import com.mpp.stellaeomphalos.content.item.RodItems.GrapnelRodItem;
import com.mpp.stellaeomphalos.content.item.RodItems.HandSpyglassItem;
import com.mpp.stellaeomphalos.content.item.RodItems.IlluminationDustItem;
import com.mpp.stellaeomphalos.content.item.RodItems.LuminaryRodItem;
import com.mpp.stellaeomphalos.content.item.RodItems.NocturnalDustItem;
import com.mpp.stellaeomphalos.content.item.RodItems.ResonanceLinkerItem;
import com.mpp.stellaeomphalos.content.item.RodItems.RosewoodBowItem;
import com.mpp.stellaeomphalos.content.item.RodItems.RunedWandItem;
import com.mpp.stellaeomphalos.content.item.RodItems.SwapperRodItem;
import com.mpp.stellaeomphalos.core.bootstrap.RegistrationGuard;
import com.mpp.stellaeomphalos.core.registry.ModItems;
import com.mpp.stellaeomphalos.data.loader.FoundationDataProvider;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 《方块物品实体完整清单》§6.2.2 物品注册总账的剩余条目：水晶族补全、星眷宝石/封印、充能工具族、符文杖族、
 * 探测与功能物品、可穿戴。
 *
 * <p>id 与类名严格对齐 §6.2.2；`mantle` / `sign_chart` 等已有实现由本类统一登记，避免与旧的
 * 通用物品循环重复注册。所有物品的堆叠与稀有度按 §6.4.5 表给定。
 */
public final class CatalogItems {

    public static final Map<String, RegistrationGuard<? extends Item>> ITEMS = new LinkedHashMap<>();

    /** 水晶族补全：天晶与已共鸣天晶（§6.2.2.2）。 */
    public static final RegistrationGuard<? extends Item> SKY_CRYSTAL =
            simple("sky_crystal", "Sky Crystal", "天晶", 1, Rarity.EPIC);

    public static final RegistrationGuard<? extends Item> RESONANT_SKY_CRYSTAL =
            simple("resonant_sky_crystal", "Resonant Sky Crystal", "共鸣天晶", 1, Rarity.EPIC);

    /** 星眷宝石三型：1.20.1 不用 meta，拆为三个独立 id（§6.4.4 契约补充）。 */
    public static final RegistrationGuard<? extends Item> BOON_GEM_SKY =
            simple("boon_gem_sky", "Boon Gem (Sky)", "星眷宝石·天", 1, Rarity.RARE);

    public static final RegistrationGuard<? extends Item> BOON_GEM_DAY =
            simple("boon_gem_day", "Boon Gem (Day)", "星眷宝石·昼", 1, Rarity.RARE);

    public static final RegistrationGuard<? extends Item> BOON_GEM_NIGHT =
            simple("boon_gem_night", "Boon Gem (Night)", "星眷宝石·夜", 1, Rarity.RARE);

    /** 星眷封印：把星眷节点 id 封进物品。 */
    public static final RegistrationGuard<? extends Item> BOON_SEAL =
            register("boon_seal", BoonSealItem::new, "Boon Seal", "星眷封印", 1, Rarity.RARE);

    /** 充能工具族（§6.2.2.3）。 */
    public static final RegistrationGuard<? extends Item> CHARGED_GEODE_AXE =
            register("charged_geode_axe", ChargedCrystalTools.Axe::new, "Charged Geode Axe", "充能水晶斧", 1, Rarity.EPIC);

    public static final RegistrationGuard<? extends Item> CHARGED_GEODE_PICKAXE =
            register("charged_geode_pickaxe", ChargedCrystalTools.Pickaxe::new, "Charged Geode Pickaxe", "充能水晶镐", 1, Rarity.EPIC);

    public static final RegistrationGuard<? extends Item> CHARGED_GEODE_SHOVEL =
            register("charged_geode_shovel", ChargedCrystalTools.Shovel::new, "Charged Geode Shovel", "充能水晶锹", 1, Rarity.EPIC);

    public static final RegistrationGuard<? extends Item> CHARGED_GEODE_SWORD =
            register("charged_geode_sword", ChargedCrystalTools.Sword::new, "Charged Geode Sword", "充能水晶剑", 1, Rarity.EPIC);

    /** 符文杖族（§6.2.2.4）。 */
    public static final RegistrationGuard<? extends Item> RUNED_WAND =
            register("runed_wand", RunedWandItem::new, "Runed Wand", "符文杖", 1, Rarity.RARE);

    public static final RegistrationGuard<? extends Item> BUILDER_ROD =
            register("builder_rod", BuilderRodItem::new, "Builder Rod", "构筑杖", 1, Rarity.RARE);

    public static final RegistrationGuard<? extends Item> SWAPPER_ROD =
            register("swapper_rod", SwapperRodItem::new, "Swapper Rod", "置换杖", 1, Rarity.RARE);

    public static final RegistrationGuard<? extends Item> LUMINARY_ROD =
            register("luminary_rod", LuminaryRodItem::new, "Luminary Rod", "照明杖", 1, Rarity.RARE);

    public static final RegistrationGuard<? extends Item> GRAPNEL_ROD =
            register("grapnel_rod", GrapnelRodItem::new, "Grapnel Rod", "抓钩杖", 1, Rarity.RARE);

    /** 探测与功能物品（§6.2.2.5）。 */
    public static final RegistrationGuard<? extends Item> HAND_SPYGLASS =
            register("spyglass_handheld", HandSpyglassItem::new, "Hand Spyglass", "手持观星镜", 1, Rarity.COMMON);

    public static final RegistrationGuard<? extends Item> SKY_RESONATOR =
            register("sky_resonator", SkyResonatorItem::new, "Sky Resonator", "天空共鸣器", 1, Rarity.RARE);

    public static final RegistrationGuard<? extends Item> RESONANCE_LINKER =
            register("resonance_linker", ResonanceLinkerItem::new, "Resonance Linker", "共鸣接线器", 1, Rarity.COMMON);

    public static final RegistrationGuard<? extends Item> ROSEWOOD_BOW =
            register("rosewood_bow", RosewoodBowItem::new, "Rosewood Bow", "玫瑰木弓", 1, Rarity.COMMON);

    public static final RegistrationGuard<? extends Item> ILLUMINATION_DUST =
            register("illumination_dust", IlluminationDustItem::new, "Illumination Dust", "辉光粉", 64, Rarity.COMMON);

    public static final RegistrationGuard<? extends Item> NOCTURNAL_DUST =
            register("nocturnal_dust", NocturnalDustItem::new, "Nocturnal Dust", "暗影粉", 64, Rarity.COMMON);

    public static final RegistrationGuard<? extends Item> STAR_GLASS =
            register("star_glass", StarGlassItem::new, "Star Glass", "星图玻璃", 1, Rarity.RARE);

    /** 可穿戴与饰品（§6.2.2.6）。 */
    public static final RegistrationGuard<? extends Item> MANTLE =
            register("mantle", StarMantleItem::new, "Star Mantle", "星披", 1, Rarity.EPIC);

    public static final RegistrationGuard<? extends Item> SIGN_CHART =
            register("sign_chart", SignChartItem::new, "Sign Chart", "星象图", 16, Rarity.COMMON);

    public static final RegistrationGuard<? extends Item> WARP_STAR =
            register("warp_star", WarpStarItem::new, "Warp Star", "迁跃之星", 1, Rarity.EPIC);

    public static final RegistrationGuard<? extends Item> WARDED_AMULET =
            register("warded_amulet", WardedAmuletItem::new, "Warded Amulet", "附魔护符", 1, Rarity.RARE);

    /** 只在物品栏内生效、由 `inventoryTick` 驱动的"星象图开盲盒"物品（§6.6.2）。 */
    public static final class SignChartItem extends Item {
        public SignChartItem() {
            super(new Properties().stacksTo(16));
        }

        @Override
        public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
            if (level.isClientSide || !(entity instanceof net.minecraft.server.level.ServerPlayer player)) return;
            if (level.getGameTime() % 40L != 0L) return;
            if (stack.hasTag() && stack.getTag().contains("SignId")) return;
            var signs = com.mpp.stellaeomphalos.constellation.sign.SignRegistry.all();
            if (signs.isEmpty()) return;
            var sign = signs.get(level.random.nextInt(signs.size()));
            stack.getOrCreateTag().putString("SignId", sign.id().toString());
            com.mpp.stellaeomphalos.player.boon.BoonProgress.getServer(player).markSeen(player, sign.id());
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "item.stellaeomphalos.sign_chart.revealed",
                            net.minecraft.network.chat.Component.translatable("sign." + sign.id().getNamespace() + "." + sign.id().getPath())),
                    false);
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
            var held = player.getItemInHand(hand);
            if (player instanceof net.minecraft.server.level.ServerPlayer server && held.hasTag()) {
                var id = net.minecraft.resources.ResourceLocation.tryParse(held.getTag().getString("SignId"));
                if (id != null && com.mpp.stellaeomphalos.constellation.sign.SignRegistry.byId(id) != null) {
                    com.mpp.stellaeomphalos.player.boon.BoonProgress.getServer(server).markSeen(server, id);
                    com.mpp.stellaeomphalos.network.SafeDispatch.send(server,
                            new com.mpp.stellaeomphalos.network.toClient.PktOpenObservation("sign_scroll", java.util.Optional.of(id)));
                }
            }
            // 客户端播翻书音并打开星象详情（界面归《客户端渲染界面与音效》）；服务端无副作用。
            if (level.isClientSide)
                level.playLocalSound(
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        0.7F,
                        1.0F,
                        false);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        }
    }

    /** 迁跃之星：长按右键写入/清除玩家共鸣星象（§6.6.2）。 */
    public static final class WarpStarItem extends Item {
        public WarpStarItem() {
            super(new Properties().stacksTo(1));
        }

        @Override
        public net.minecraft.world.InteractionResultHolder<ItemStack> use(
                net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
            var stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server) {
                var tag = stack.getOrCreateTag();
                if (tag.contains("SignId") && server.isShiftKeyDown()) {
                    tag.remove("SignId");
                } else if (tag.contains("SignId")) {
                    var sign = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("SignId"));
                    if (sign != null)
                        // §6.2.2.5 / §6.6.2：写入玩家共鸣星象；BoonProgress 是共鸣状态的唯一权威。
                        com.mpp.stellaeomphalos.player.boon.BoonProgress.getServer(server)
                                .setAttuned(server, sign);
                }
            }
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    /** 附魔护符：首次装备时惰性掷出附魔修正（§6.3.5 / §6.6.2）。 */
    public static final class WardedAmuletItem extends net.minecraft.world.item.Item {
        public WardedAmuletItem() {
            super(new Properties().stacksTo(1));
        }

        @Override
        public void inventoryTick(
                ItemStack stack,
                net.minecraft.world.level.Level level,
                net.minecraft.world.entity.Entity entity,
                int slot,
                boolean selected) {
            if (level.isClientSide || !(entity instanceof net.minecraft.server.level.ServerPlayer player))
                return;
            if (level.getGameTime() % 20L != 0L) return;
            com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder.rollIfNeeded(
                    stack,
                    level.random,
                    candidateEnchantments(),
                    candidate -> 0,
                    player.getUUID());
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                net.minecraft.world.level.Level level,
                java.util.List<net.minecraft.network.chat.Component> tooltip,
                net.minecraft.world.item.TooltipFlag flag) {
            var modifiers = com.mpp.stellaeomphalos.content.item.amulet.AmuletHolder.modifiers(stack);
            if (modifiers.isEmpty()) {
                tooltip.add(
                        net.minecraft.network.chat.Component.translatable(
                                        "item.stellaeomphalos.warded_amulet.inert")
                                .withStyle(net.minecraft.ChatFormatting.GRAY));
                return;
            }
            for (var modifier : modifiers)
                tooltip.add(
                        net.minecraft.network.chat.Component.literal(" - " + modifier.describe())
                                .withStyle(net.minecraft.ChatFormatting.AQUA));
        }
    }

    /**
     * 可作为护符目标的原版附魔 id 列表。
     *
     * <p>排除诅咒与宝藏类附魔的需要由数据包 tag 表达；这里给出的是注册表快照，
     * 因此只在服务端 tick 内调用。
     */
    public static java.util.List<net.minecraft.resources.ResourceLocation> candidateEnchantments() {
        var result = new java.util.ArrayList<net.minecraft.resources.ResourceLocation>();
        for (var enchantment : net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValues()) {
            if (enchantment.isCurse()) continue;
            var id = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (id != null) result.add(id);
        }
        return java.util.List.copyOf(result);
    }

    /** 星眷封印：保存星眷节点 id。 */
    public static final class BoonSealItem extends Item {
        public BoonSealItem() {
            super(new Properties().stacksTo(1));
        }

        /** 写入封印指向的星眷节点。 */
        public static void seal(ItemStack stack, net.minecraft.resources.ResourceLocation boon) {
            stack.getOrCreateTag().putString("Boon", boon.toString());
        }

        /** @return 封印指向的星眷节点；未封印时为 null */
        public static net.minecraft.resources.ResourceLocation sealed(ItemStack stack) {
            if (!stack.hasTag()) return null;
            return net.minecraft.resources.ResourceLocation.tryParse(stack.getTag().getString("Boon"));
        }
    }

    private CatalogItems() {}

    public static void initialize() {}

    /** 注册发行版生命周期钩子；发射器行为在公共设置阶段注册，避免加载期访问注册表。 */
    public static void attach(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener(CatalogItems::onCommonSetup);
    }

    private static void onCommonSetup(
            net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(CatalogItems::registerDispenserBehaviours);
    }

    /**
     * 粉尘的发射器行为（《方块物品实体完整清单》§6.6.2）：按发射器朝向出射，辉光粉就地放光、暗影粉生成刷新火花，
     * **成功才扣 1**。
     */
    public static void registerDispenserBehaviours() {
        net.minecraft.core.dispenser.DispenseItemBehavior lucent =
                (source, stack) -> {
                    var level = source.getLevel();
                    var facing =
                            level.getBlockState(source.getPos())
                                    .getValue(
                                            net.minecraft.world.level.block.DispenserBlock
                                                    .FACING);
                    if (!com.mpp.stellaeomphalos.content.entity.catalog.LucentSparkEntity.placeLightAt(
                            level, source.getPos().relative(facing))) return stack;
                    stack.shrink(1);
                    return stack;
                };
        net.minecraft.core.dispenser.DispenseItemBehavior umbral =
                (source, stack) -> {
                    var level = source.getLevel();
                    var facing =
                            level.getBlockState(source.getPos())
                                    .getValue(
                                            net.minecraft.world.level.block.DispenserBlock
                                                    .FACING);
                    var target = source.getPos().relative(facing);
                    var spark =
                            new com.mpp.stellaeomphalos.content.entity.catalog.UmbralSparkEntity(
                                    com.mpp.stellaeomphalos.content.entity.catalog.CatalogEntities
                                            .UMBRAL_SPARK
                                            .get(),
                                    null,
                                    level);
                    spark.setSpawning(true);
                    spark.setPos(
                            target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
                    spark.shoot(
                            facing.getStepX(), facing.getStepY(), facing.getStepZ(), 0.7F, 0.9F);
                    if (!level.addFreshEntity(spark)) return stack;
                    stack.shrink(1);
                    return stack;
                };
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(
                ILLUMINATION_DUST.get(), lucent);
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(
                NOCTURNAL_DUST.get(), umbral);
    }

    /** 掉落时替换为晶簇实体的物品 id（§6.6.2：任意水晶/宝石）。 */
    private static final java.util.Set<String> CRYSTAL_OR_GEM_IDS =
            java.util.Set.of(
                    "geode",
                    "sky_crystal",
                    "resonant_geode",
                    "resonant_sky_crystal",
                    "warp_star",
                    "boon_gem_sky",
                    "boon_gem_day",
                    "boon_gem_night");

    /** 掉落时替换为晶簇工具实体的物品 id。 */
    private static final java.util.Set<String> CRYSTAL_TOOL_IDS =
            java.util.Set.of(
                    "charged_geode_axe",
                    "charged_geode_pickaxe",
                    "charged_geode_shovel",
                    "charged_geode_sword");

    /** @return 该物品是否属于水晶/宝石族（按注册 id 判定，避免依赖注册守卫的加载时机） */
    public static boolean isCrystalOrGem(net.minecraft.world.item.Item item) {
        return hasId(item, CRYSTAL_OR_GEM_IDS);
    }

    /** @return 该物品是否属于充能水晶工具族 */
    public static boolean isCrystalTool(net.minecraft.world.item.Item item) {
        return hasId(item, CRYSTAL_TOOL_IDS) || isPlainTool(item);
    }

    private static boolean isPlainTool(net.minecraft.world.item.Item item) {
        return hasId(
                item,
                java.util.Set.of("geode_axe", "geode_pickaxe", "geode_shovel", "geode_sword"));
    }

    private static boolean hasId(net.minecraft.world.item.Item item, java.util.Set<String> ids) {
        if (item == null) return false;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        return id != null && id.getNamespace().equals(Omphalos.MODID) && ids.contains(id.getPath());
    }

    static {
        // 运行时构造的提示与模式名（§6.4.6 / §6.6.2）。
        lang("item.stellaeomphalos.sky_resonator.mode.starlight", "Mode: Starlight", "模式：星辉场");
        lang("item.stellaeomphalos.sky_resonator.mode.fluid_fields", "Mode: Fluid Fields", "模式：流体分布");
        lang("item.stellaeomphalos.sky_resonator.mode.area_size", "Mode: Area Size", "模式：作用半径");
        lang("item.stellaeomphalos.runed_wand.augment", "Augment %s", "增幅 %s");
        lang("item.stellaeomphalos.block_rod.stored", "Stored block states: %s", "已存方块状态：%s");
        lang("item.stellaeomphalos.star_glass.blank", "Unengraved", "未雕刻");
        lang("item.stellaeomphalos.star_glass.engraved", "Engraved signs: %s", "已雕刻星象：%s");
        lang("item.stellaeomphalos.sign_chart.revealed", "Chart revealed: %s", "图卷显现：%s");
        lang("item.stellaeomphalos.warded_amulet.inert", "Inert amulet", "未激活的护符");
        lang("item.stellaeomphalos.runed_wand.augment.none", "Augment: none", "增幅：无");
        lang("item.stellaeomphalos.runed_wand.augment.aevitas", "Augment: paving", "增幅：铺路");
        lang("item.stellaeomphalos.runed_wand.augment.vicio", "Augment: leap", "增幅：跃迁");
        lang("item.stellaeomphalos.runed_wand.augment.armara", "Augment: ward", "增幅：御盾");
        lang("item.stellaeomphalos.runed_wand.augment.prospect", "Augment: prospect", "增幅：寻矿");
        lang("item.stellaeomphalos.luminary_rod.color", "Colour: %s", "光色：%s");
        lang("item.stellaeomphalos.resonance_linker.first", "Link origin set", "已记录链接起点");
        lang("item.stellaeomphalos.resonance_linker.linked", "Linked", "已建立链接");
        lang("item.stellaeomphalos.resonance_linker.failed", "Link failed (peer already linked)", "链接失败（对端已链接）");
    }

    private static void lang(String key, String english, String chinese) {
        FoundationDataProvider.language(key, english, chinese);
    }

    private static RegistrationGuard<? extends Item> simple(
            String id, String english, String chinese, int stack, Rarity rarity) {
        return register(id, () -> new Item(new Item.Properties().stacksTo(stack).rarity(rarity)), english, chinese, stack, rarity, true);
    }

    private static <T extends Item> RegistrationGuard<? extends Item> register(
            String id, java.util.function.Supplier<T> factory, String english, String chinese, int stack, Rarity rarity) {
        return register(id, factory, english, chinese, stack, rarity, true);
    }

    private static <T extends Item> RegistrationGuard<? extends Item> register(
            String id,
            java.util.function.Supplier<T> factory,
            String english,
            String chinese,
            int stack,
            Rarity rarity,
            boolean announce) {
        var guard = ModItems.ENTRIES.declare(id, factory);
        ITEMS.put(id, guard);
        if (announce)
            FoundationDataProvider.language("item." + Omphalos.MODID + "." + id, english, chinese);
        return guard;
    }
}
