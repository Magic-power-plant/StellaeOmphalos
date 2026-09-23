package com.mpp.stellaeomphalos.knowledge.codex;

import com.mpp.stellaeomphalos.Omphalos;
import com.mpp.stellaeomphalos.core.platform.StarTier;
import com.mpp.stellaeomphalos.knowledge.research.*;
import com.mpp.stellaeomphalos.knowledge.shard.*;

import net.minecraft.resources.ResourceLocation;

import java.util.*;

/** Original editorial structure; recipe ids refer to the project's own 《星坛与制作系统》/《星构多方块与星仪世界生成》 content. */
public final class KnowledgeCatalog {
    public static final CodexRegistry PAGES = new CodexRegistry();
    public static final StudyNodeRegistry NODES = new StudyNodeRegistry();
    public static final ShardPool SHARDS = new ShardPool();
    private static final Map<String, List<String>> LANGUAGE = new LinkedHashMap<>();
    private static final Map<ResourceLocation, StarTier> RECIPE_TIERS = new LinkedHashMap<>();
    private static final EnumMap<StudyBranch, Integer> COUNTS = new EnumMap<>(StudyBranch.class);
    private static boolean initialized;

    private KnowledgeCatalog() {}

    public static ResourceLocation id(String path) {
        return new ResourceLocation(Omphalos.MODID, path);
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        for (var branch : StudyBranch.values()) {
            String key = "codex.stellaeomphalos.branch." + branch.name().toLowerCase(Locale.ROOT);
            language(
                    key,
                    switch (branch) {
                        case DISCOVERY -> "Discovery";
                        case BASIC_CRAFT -> "First Works";
                        case ATTUNEMENT -> "Resonance";
                        case CONSTELLATION -> "Celestial Patterns";
                        case RADIANCE -> "Radiance";
                        case BRILLIANCE -> "Brilliance";
                    },
                    switch (branch) {
                        case DISCOVERY -> "初识";
                        case BASIC_CRAFT -> "造物";
                        case ATTUNEMENT -> "共鸣";
                        case CONSTELLATION -> "星象";
                        case RADIANCE -> "辉耀";
                        case BRILLIANCE -> "璀璨";
                    });
            add(
                    "chapter/" + branch.name().toLowerCase(Locale.ROOT),
                    branch,
                    "minecraft:written_book",
                    PageKind.PROGRESS,
                    "",
                    "Your next step",
                    "下一步",
                    switch (branch) {
                        case DISCOVERY ->
                                "Explore marble shrines and collect crystal materials. Craft your"
                                    + " first discovery altar to begin working with lumen.";
                        case BASIC_CRAFT ->
                                "Build an altar, expose it to the night sky and supply its recipe"
                                    + " grid. Completing an upgraded altar opens the next chapter.";
                        case ATTUNEMENT ->
                                "Discover a major celestial sign, tune your crystal and align the"
                                    + " resonance frame. Your root boon never consumes a skill"
                                    + " point.";
                        case CONSTELLATION ->
                                "Use a sign focus and the outer altar slots. Study a blueprint"
                                    + " before requesting a projection; a projection never places"
                                    + " blocks.";
                        case RADIANCE ->
                                "Bind trait effects into equipment and refine your ritual"
                                    + " structures. Mantle state belongs to the item and travels"
                                    + " with it.";
                        case BRILLIANCE ->
                                "Develop your boon tree and share your discoveries. An insight"
                                    + " scroll shares knowledge without transferring experience or"
                                    + " skill points.";
                    },
                    switch (branch) {
                        case DISCOVERY -> "探索大理石遗迹，收集水晶材料。合成初识星之祭坛，开始使用星辉。";
                        case BASIC_CRAFT -> "让星之祭坛能够观测夜空，按配方摆放材料。完成星之祭坛升级可开启下一研习支。";
                        case ATTUNEMENT -> "发现主星象、调谐水晶并搭建共鸣结构。根眷不消耗技能点。";
                        case CONSTELLATION -> "使用星象焦点与外环槽位。先研读蓝图再请求投影；投影不会放置方块。";
                        case RADIANCE -> "把特性融入装备，完善仪式。星披状态保存在物品上，转交装备时一并转移。";
                        case BRILLIANCE -> "完善星眷树并分享见闻。授识卷轴只传递知识，不传递经验和技能点。";
                    });
        }
        for (var row : RECIPE_ROWS.strip().split("\\R")) {
            var fields = row.strip().split("\\|");
            var tier =
                    switch (fields[2]) {
                        case "basic_craft" -> StarTier.BASIC_CRAFT;
                        case "resonance" -> StarTier.ATTUNEMENT;
                        case "sign" -> StarTier.CONSTELLATION;
                        case "radiance", "trait" -> StarTier.RADIANCE;
                        default -> StarTier.DISCOVERY;
                    };
            var recipe = id(fields[0]);
            RECIPE_TIERS.put(recipe, tier);
            String name = fields[0].substring(fields[0].lastIndexOf('/') + 1).replace('_', ' ');
            add(
                    "recipe/" + fields[0],
                    StudyBranch.valueOf(tier.name()),
                    fields[3],
                    PageKind.valueOf(fields[1]),
                    recipe.toString(),
                    name,
                    "制作：" + CatalogChinese.recipe(fields[0]),
                    "Recipe: "
                            + recipe
                            + "\nFollow the ingredient arrangement on the facing page. Material"
                            + " alternatives rotate every second. The server checks the required"
                            + " chapter, machine frame, focus and lumen before crafting.",
                    "配方：" + recipe + "\n按右页摆放材料；可替代材料每秒轮换。制作前服务端检查研习进度、机器结构、焦点与星辉条件。");
        }
        for (String sign :
                List.of(
                        "aevitas",
                        "armara",
                        "discidia",
                        "evorsio",
                        "vicio",
                        "bootes",
                        "fornax",
                        "horologium",
                        "lucerna",
                        "mineralis",
                        "octans",
                        "pelotrio",
                        "gelu",
                        "ulteria",
                        "alcara",
                        "vorux")) {
            add(
                    "sign/" + sign,
                    StudyBranch.ATTUNEMENT,
                    "stellaeomphalos:sign_chart",
                    PageKind.CELESTIAL,
                    id(sign).toString(),
                    "Pattern: " + sign,
                    "星图：" + CatalogChinese.sign(sign),
                    "Observe this pattern with your sky instrument. Its visible nights follow the"
                        + " world's celestial cycle.",
                    "使用观测仪器记录该星图。可见时段由存档中的星象周期决定。");
            for (String topic :
                    List.of("sighting", "alignment", "craft", "rite", "mantle", "affinity")) {
                var gate =
                        CodexGate.all(
                                CodexGate.tier(StarTier.ATTUNEMENT),
                                new CodexGate(
                                        "sign", id(sign).toString(), List.of(), GateLevel.HIDDEN));
                shard(
                        "sign/" + sign + "/" + topic,
                        "Notes on " + sign + ": " + topic,
                        CatalogChinese.sign(sign) + "·" + CatalogChinese.topic(topic),
                        "A recorded observation connects "
                                + sign
                                + " with "
                                + topic
                                + ". Check the sign page and the available recipes before arranging"
                                + " an experiment.",
                        "这份记录连接了星象 " + CatalogChinese.sign(sign) + " 与主题 " + CatalogChinese.topic(topic) + "。进行试验前请检查星象页和已开放的配方。",
                        gate,
                        id(sign).toString());
            }
        }
        for (String blueprint :
                List.of(
                        "pattern_altar_t2",
                        "pattern_altar_t3",
                        "pattern_altar_t4",
                        "pattern_starlight_infuser",
                        "pattern_fountain",
                        "pattern_rite_pedestal",
                        "pattern_rite_pedestal_with_link",
                        "pattern_resonance_relay",
                        "pattern_gateway",
                        "pattern_crystal_enhancement"))
            add(
                    "structure/" + blueprint,
                    StudyBranch.ATTUNEMENT,
                    "stellaeomphalos:marble",
                    PageKind.STRUCTURE,
                    id(blueprint).toString(),
                    "Structure: " + blueprint.replace("pattern_", "").replace('_', ' '),
                    "星构：" + blueprint,
                    "Compare each layer with the blueprint. Missing or unloaded cells prevent"
                        + " activation; a projection is a guide and consumes no blocks.",
                    "逐层对照蓝图搭建。结构缺失或区块未加载会阻止启动；投影仅供参考，不会消耗或放置方块。");
        for (String root : List.of("aevitas", "armara", "discidia", "evorsio", "vicio"))
            add(
                    "boon/" + root,
                    StudyBranch.ATTUNEMENT,
                    "minecraft:nether_star",
                    PageKind.BOON,
                    id(root + "/root").toString(),
                    "Root affinity: " + root,
                    "根眷：" + root,
                    "Resonance selects your root. Earn experience through its associated actions;"
                        + " linked nodes consume points, while the root is free.",
                    "共鸣决定根眷。通过相关行动获得经验；连接节点消耗眷点，根眷免费。");
        add(
                "lore/index",
                StudyBranch.DISCOVERY,
                "stellaeomphalos:lore_shard",
                PageKind.INDEX,
                "",
                "Lore archive",
                "残片文库",
                "Open a capsule to receive a seeded shard. Reveal it to add its knowledge to this"
                    + " world's archive.",
                "开启封壳获得带种子的残片，再揭示内容以收入本存档的文库。");
        add(
                "layout/blank",
                StudyBranch.DISCOVERY,
                "minecraft:paper",
                PageKind.BLANK,
                "",
                "Notes",
                "笔记",
                "A quiet space between discoveries.",
                "在发现之间留下一页空白。");
        for (int i = 0; i < 14; i++) {
            var node =
                    NODES.all().stream()
                            .filter(n -> n.branch() == StudyBranch.DISCOVERY)
                            .skip(i)
                            .findFirst()
                            .orElseThrow();
            shard(
                    "study/" + node.id().getPath(),
                    "Field note " + (i + 1),
                    "观测札记 " + (i + 1),
                    "Begin with the simplest working arrangement. Record a successful result before"
                        + " changing a second variable.",
                    "先完成最简单的可用装置，记录一次成功结果后，再改变下一项条件。",
                    CodexGate.tier(StarTier.DISCOVERY),
                    "");
        }
        NODES.validate();
    }

    private static void add(
            String slug,
            StudyBranch branch,
            String icon,
            PageKind kind,
            String reference,
            String enTitle,
            String zhTitle,
            String enBody,
            String zhBody) {
        String title = "codex.stellaeomphalos." + slug.replace('/', '.') + ".title",
                body = title.replace(".title", ".body");
        language(title, enTitle, zhTitle);
        language(body, enBody, zhBody);
        var gate = CodexGate.tier(branch.requiredTier());
        ResourceLocation first = id("codex/" + slug + "/text/0"),
                second = id("codex/" + slug + "/" + kind.name().toLowerCase(Locale.ROOT) + "/1");
        PAGES.register(
                first,
                new CodexPage(PageKind.TEXT, title, body, "", "", gate, false, List.of(0, 0, 0)),
                slug.replace('/', '_') + "_text");
        PAGES.register(
                second,
                new CodexPage(
                        kind,
                        title,
                        "",
                        reference,
                        branch.name(),
                        gate,
                        kind == PageKind.STRUCTURE,
                        List.of(0, 0, 0)),
                slug.replace('/', '_'));
        int index = COUNTS.merge(branch, 1, Integer::sum) - 1;
        var prerequisites =
                slug.startsWith("chapter/")
                        ? List.<ResourceLocation>of()
                        : List.of(id("chapter/" + branch.name().toLowerCase(Locale.ROOT)));
        NODES.register(
                new StudyNode(
                        id(slug),
                        branch,
                        index % 6 * 36,
                        index / 6 * 36,
                        new ResourceLocation(icon),
                        prerequisites,
                        List.of(first, second),
                        gate,
                        false));
    }

    private static void shard(
            String slug,
            String enTitle,
            String zhTitle,
            String enBody,
            String zhBody,
            CodexGate gate,
            String sign) {
        String base = "lore.stellaeomphalos." + slug.replace('/', '.');
        language(base + ".name", enTitle, zhTitle);
        language(base + ".ribbon", enTitle, zhTitle);
        language(base + ".body", enBody, zhBody);
        SHARDS.register(
                new LoreShard(
                        id("lore/" + slug),
                        base + ".name",
                        base + ".ribbon",
                        base + ".body",
                        gate,
                        gate,
                        "LORE_INDEX",
                        sign));
    }

    public static Map<String, List<String>> languages() {
        return Map.copyOf(LANGUAGE);
    }

    public static void language(String key, String en, String zh) {
        LANGUAGE.put(key, List.of(en, zh));
    }

    public static StarTier recipeTier(ResourceLocation id) {
        return RECIPE_TIERS.getOrDefault(id, StarTier.DISCOVERY);
    }

    public static boolean localized(String key) {
        return LANGUAGE.containsKey(key) && LANGUAGE.get(key).stream().allMatch(s -> !s.isBlank());
    }

    private static final String RECIPE_ROWS =
            """
altar/charm_reroll|RECIPE_ALTAR|sign|stellaeomphalos:warded_amulet
altar/collector_crystal|RECIPE_ALTAR|sign|stellaeomphalos:collector_crystal
altar/warp_star|RECIPE_ALTAR|discovery|stellaeomphalos:warp_star
altar/beam_relay|RECIPE_ALTAR|discovery|stellaeomphalos:beam_relay
altar/geode_axe|RECIPE_ALTAR|resonance|stellaeomphalos:geode_axe
altar/geode_pickaxe|RECIPE_ALTAR|resonance|stellaeomphalos:geode_pickaxe
altar/geode_shovel|RECIPE_ALTAR|resonance|stellaeomphalos:geode_shovel
altar/geode_sword|RECIPE_ALTAR|resonance|stellaeomphalos:geode_sword
altar/drill_deep|RECIPE_ALTAR|trait|stellaeomphalos:drill_head
altar/drill_head|RECIPE_ALTAR|discovery|stellaeomphalos:drill_head
altar/drill_piercing|RECIPE_ALTAR|trait|stellaeomphalos:drill_head
altar/drill_swift|RECIPE_ALTAR|trait|stellaeomphalos:drill_head
altar/drill_wide|RECIPE_ALTAR|trait|stellaeomphalos:drill_head
altar/warded_amulet|RECIPE_ALTAR|discovery|stellaeomphalos:warded_amulet
altar/quern|RECIPE_ALTAR|discovery|stellaeomphalos:quern
altar/luminary_rod|RECIPE_ALTAR|discovery|stellaeomphalos:luminary_rod
altar/luminary_rod_aevitas|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_alcara|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_armara|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_bootes|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_discidia|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_evorsio|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_fornax|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_gelu|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_horologium|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_lucerna|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_mineralis|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_octans|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_pelotrio|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_ulteria|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_vicio|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/luminary_rod_vorux|RECIPE_ALTAR|sign|stellaeomphalos:luminary_rod
altar/lens_black|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_blue|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_brown|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_cyan|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_gray|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_green|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_light_blue|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_light_gray|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_lime|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_magenta|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_orange|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_pink|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_purple|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_red|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_white|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/lens_yellow|RECIPE_ALTAR|resonance|stellaeomphalos:lens_blank
altar/light_transmuter|RECIPE_ALTAR|discovery|stellaeomphalos:light_transmuter
altar/lumen_battery|RECIPE_ALTAR|discovery|stellaeomphalos:lumen_battery
altar/chalice|RECIPE_ALTAR|discovery|stellaeomphalos:chalice
altar/collector|RECIPE_ALTAR|discovery|stellaeomphalos:collector
altar/lumen_flask|RECIPE_ALTAR|discovery|stellaeomphalos:lumen_flask
altar/lumen_infuser|RECIPE_ALTAR|discovery|stellaeomphalos:lumen_infuser
altar/lumen_relay|RECIPE_ALTAR|discovery|stellaeomphalos:lumen_relay
altar/lumen_well|RECIPE_ALTAR|discovery|stellaeomphalos:lumen_well
altar/prism_lens|RECIPE_ALTAR|discovery|stellaeomphalos:prism_lens
altar/geode|RECIPE_ALTAR|discovery|stellaeomphalos:geode
altar/sky_resonator|RECIPE_ALTAR|discovery|stellaeomphalos:sky_resonator
altar/sky_resonator_precision|RECIPE_ALTAR|sign|stellaeomphalos:sky_resonator
altar/sky_resonator_range|RECIPE_ALTAR|sign|stellaeomphalos:sky_resonator
altar/ritual_base|RECIPE_ALTAR|resonance|stellaeomphalos:ritual_base
altar/sextant_upgrade|RECIPE_ALTAR|sign|stellaeomphalos:star_sextant
altar/sign_focus_aevitas|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_alcara|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_armara|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_bootes|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_discidia|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_evorsio|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_fornax|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_gelu|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_horologium|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_lucerna|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_mineralis|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_octans|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_pelotrio|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_ulteria|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_vicio|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_focus_vorux|RECIPE_ALTAR|resonance|stellaeomphalos:sign_focus
altar/sign_chart_aevitas|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_alcara|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_armara|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_bootes|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_discidia|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_evorsio|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_fornax|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_gelu|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_horologium|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_lucerna|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_mineralis|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_octans|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_pelotrio|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_ulteria|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_vicio|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/sign_chart_vorux|RECIPE_ALTAR|sign|stellaeomphalos:sign_chart
altar/lens_blank|RECIPE_ALTAR|discovery|stellaeomphalos:lens_blank
altar/mantle|RECIPE_ALTAR|discovery|stellaeomphalos:mantle
altar/mantle_aevitas|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_alcara|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_armara|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_bootes|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_discidia|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_evorsio|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_fornax|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_gelu|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_horologium|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_lucerna|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_mineralis|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_octans|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_pelotrio|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_ulteria|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_vicio|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/mantle_vorux|RECIPE_ALTAR|sign|stellaeomphalos:mantle
altar/star_sextant|RECIPE_ALTAR|discovery|stellaeomphalos:star_sextant
altar/upgrade_resonance|RECIPE_ALTAR|discovery|stellaeomphalos:asterism_altar_resonance
altar/upgrade_sign|RECIPE_ALTAR|resonance|stellaeomphalos:asterism_altar_sign
altar/upgrade_trait|RECIPE_ALTAR|sign|stellaeomphalos:asterism_altar_trait
altar/upgrade_radiance|RECIPE_ALTAR|trait|stellaeomphalos:asterism_altar_radiance
grinding/alter_crystal|RECIPE|discovery|minecraft:crafting_table
grinding/alter_hone|RECIPE|discovery|minecraft:crafting_table
grinding/alter_tool|RECIPE|discovery|minecraft:crafting_table
grinding/amethyst_block|RECIPE|discovery|minecraft:amethyst_shard
grinding/blaze_rod|RECIPE|discovery|minecraft:blaze_powder
grinding/bone|RECIPE|discovery|minecraft:bone_meal
grinding/clay|RECIPE|discovery|minecraft:clay_ball
grinding/cobblestone|RECIPE|discovery|minecraft:gravel
grinding/glowstone|RECIPE|discovery|minecraft:glowstone_dust
grinding/gravel|RECIPE|discovery|minecraft:sand
grinding/quartz_block|RECIPE|discovery|minecraft:quartz
grinding/red_sandstone|RECIPE|discovery|minecraft:red_sand
grinding/sandstone|RECIPE|discovery|minecraft:sand
grinding/stone|RECIPE|discovery|minecraft:cobblestone
grinding/wheat|RECIPE|discovery|minecraft:wheat_seeds
infusion/charged_axe|RECIPE|discovery|stellaeomphalos:geode_axe
infusion/charged_pickaxe|RECIPE|discovery|stellaeomphalos:geode_pickaxe
infusion/charged_shovel|RECIPE|discovery|stellaeomphalos:geode_shovel
infusion/charged_sword|RECIPE|discovery|stellaeomphalos:geode_sword
infusion/geode|RECIPE|discovery|stellaeomphalos:geode
infusion/resonant_geode|RECIPE|discovery|stellaeomphalos:resonant_geode
interaction/crystal|RECIPE|discovery|stellaeomphalos:geode
interaction/lava|RECIPE|discovery|minecraft:crafting_table
interaction/water|RECIPE|discovery|minecraft:crafting_table
liquefaction/catalyst_0|RECIPE|discovery|minecraft:crafting_table
liquefaction/catalyst_1|RECIPE|discovery|minecraft:crafting_table
liquefaction/catalyst_2|RECIPE|discovery|minecraft:crafting_table
liquefaction/catalyst_3|RECIPE|discovery|minecraft:crafting_table
liquefaction/catalyst_4|RECIPE|discovery|minecraft:crafting_table
liquefaction/catalyst_5|RECIPE|discovery|minecraft:crafting_table
melting/black_glass|RECIPE|discovery|minecraft:crafting_table
melting/blue_glass|RECIPE|discovery|minecraft:crafting_table
melting/brown_glass|RECIPE|discovery|minecraft:crafting_table
melting/cyan_glass|RECIPE|discovery|minecraft:crafting_table
melting/gray_glass|RECIPE|discovery|minecraft:crafting_table
melting/green_glass|RECIPE|discovery|minecraft:crafting_table
melting/ice|RECIPE|discovery|minecraft:crafting_table
melting/light_blue_glass|RECIPE|discovery|minecraft:crafting_table
melting/light_gray_glass|RECIPE|discovery|minecraft:crafting_table
melting/lime_glass|RECIPE|discovery|minecraft:crafting_table
melting/magenta_glass|RECIPE|discovery|minecraft:crafting_table
melting/orange_glass|RECIPE|discovery|minecraft:crafting_table
melting/pink_glass|RECIPE|discovery|minecraft:crafting_table
melting/purple_glass|RECIPE|discovery|minecraft:crafting_table
melting/red_glass|RECIPE|discovery|minecraft:crafting_table
melting/white_glass|RECIPE|discovery|minecraft:crafting_table
melting/yellow_glass|RECIPE|discovery|minecraft:crafting_table
transmutation/black_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/blue_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/brown_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/cyan_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/gray_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/green_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/light_blue_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/light_gray_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/lime_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/magenta_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/orange_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/pink_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/purple_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/red_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/white_concrete|RECIPE|discovery|minecraft:crafting_table
transmutation/yellow_concrete|RECIPE|discovery|minecraft:crafting_table
workbench/discovery_altar|RECIPE|discovery|stellaeomphalos:asterism_altar_discovery
workbench/light_wand|RECIPE_LIGHT|discovery|stellaeomphalos:luminary_rod
workbench/wand_recolor|RECIPE|discovery|minecraft:crafting_table
world/aquamarine_sand|RECIPE|basic_craft|stellaeomphalos:aquamarine_sand
world/sky_crystal_cluster|RECIPE|basic_craft|stellaeomphalos:sky_crystal_cluster
world/fountain|RECIPE|basic_craft|stellaeomphalos:fountain
world/bore_head_diamond|RECIPE|basic_craft|stellaeomphalos:bore_head_diamond
world/bore_head_iron|RECIPE|basic_craft|stellaeomphalos:bore_head_iron
world/bore_head_stone|RECIPE|basic_craft|stellaeomphalos:bore_head_stone
world/black_marble|RECIPE|basic_craft|stellaeomphalos:black_marble
world/black_marble_arch|RECIPE|basic_craft|stellaeomphalos:black_marble_arch
world/black_marble_bricks|RECIPE|basic_craft|stellaeomphalos:black_marble_bricks
world/black_marble_chiseled|RECIPE|basic_craft|stellaeomphalos:black_marble_chiseled
world/black_marble_engraved|RECIPE|basic_craft|stellaeomphalos:black_marble_engraved
world/black_marble_pillar|RECIPE|basic_craft|stellaeomphalos:black_marble_pillar
world/black_marble_runed|RECIPE|basic_craft|stellaeomphalos:black_marble_runed
world/gate_core|RECIPE|basic_craft|stellaeomphalos:gate_core
world/prism_crystal_cluster|RECIPE|basic_craft|stellaeomphalos:prism_crystal_cluster
world/geode_ore|RECIPE|basic_craft|stellaeomphalos:geode_ore
world/glowbloom|RECIPE|basic_craft|stellaeomphalos:glowbloom
world/infused_wood|RECIPE|basic_craft|stellaeomphalos:infused_wood
world/infused_wood_arch|RECIPE|basic_craft|stellaeomphalos:infused_wood_arch
world/infused_wood_planks|RECIPE|basic_craft|stellaeomphalos:infused_wood_planks
world/infused_wood_enriched|RECIPE|basic_craft|stellaeomphalos:infused_wood_enriched
world/infused_wood_engraved|RECIPE|basic_craft|stellaeomphalos:infused_wood_engraved
world/infused_wood_pillar|RECIPE|basic_craft|stellaeomphalos:infused_wood_pillar
world/infused_wood_infused|RECIPE|basic_craft|stellaeomphalos:infused_wood_infused
world/lumen_spring|RECIPE|basic_craft|stellaeomphalos:lumen_spring
world/ore_regenerator|RECIPE|basic_craft|stellaeomphalos:ore_regenerator
world/observatory|RECIPE|basic_craft|stellaeomphalos:observatory
world/rite_amplifier_attuned|RECIPE|basic_craft|stellaeomphalos:rite_amplifier_attuned
world/rite_amplifier_crude|RECIPE|basic_craft|stellaeomphalos:rite_amplifier_crude
world/rite_amplifier_polished|RECIPE|basic_craft|stellaeomphalos:rite_amplifier_polished
world/rite_amplifier_resonant|RECIPE|basic_craft|stellaeomphalos:rite_amplifier_resonant
world/rite_link|RECIPE|basic_craft|stellaeomphalos:rite_link
world/rite_pedestal|RECIPE|basic_craft|stellaeomphalos:rite_pedestal
world/marble|RECIPE|basic_craft|stellaeomphalos:marble
world/marble_arch|RECIPE|basic_craft|stellaeomphalos:marble_arch
world/marble_bricks|RECIPE|basic_craft|stellaeomphalos:marble_bricks
world/marble_chiseled|RECIPE|basic_craft|stellaeomphalos:marble_chiseled
world/marble_engraved|RECIPE|basic_craft|stellaeomphalos:marble_engraved
world/marble_pillar|RECIPE|basic_craft|stellaeomphalos:marble_pillar
world/marble_runed|RECIPE|basic_craft|stellaeomphalos:marble_runed
world/luminaire|RECIPE|basic_craft|stellaeomphalos:luminaire
""";
}
