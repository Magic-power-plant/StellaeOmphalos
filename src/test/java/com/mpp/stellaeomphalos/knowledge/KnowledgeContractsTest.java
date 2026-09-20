package com.mpp.stellaeomphalos.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mpp.stellaeomphalos.client.codex.*;
import com.mpp.stellaeomphalos.core.platform.*;
import com.mpp.stellaeomphalos.knowledge.advancement.*;
import com.mpp.stellaeomphalos.knowledge.codex.*;
import com.mpp.stellaeomphalos.knowledge.research.*;
import com.mpp.stellaeomphalos.knowledge.shard.*;
import com.mpp.stellaeomphalos.network.toServer.*;
import com.mpp.stellaeomphalos.player.progress.*;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.*;

class KnowledgeContractsTest {
    static ResourceLocation id(String p) {
        return new ResourceLocation("stellaeomphalos", p);
    }

    static CodexGate gate(String json) {
        return CodexGate.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, s -> fail(s));
    }

    @Test
    void initialVisibilityContainsExactlyTheDiscoveryBranch() {
        KnowledgeCatalog.initialize();
        var context = GateContext.of(new StarRecord());
        assertEquals(
                KnowledgeCatalog.NODES.all().stream()
                        .filter(n -> n.branch() == StudyBranch.DISCOVERY)
                        .map(StudyNode::id)
                        .collect(java.util.stream.Collectors.toSet()),
                KnowledgeCatalog.NODES.all().stream()
                        .filter(n -> n.visibility(context).active())
                        .map(StudyNode::id)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void gatesArePureFrozenAndComposeAllAnyNot() {
        var record = new StarRecord();
        var context = GateContext.of(record);
        var required = gate("{\"tier\":\"ATTUNEMENT\"}");
        var before = record.save();
        assertEquals(GateLevel.SILHOUETTE, required.evaluate(context).level());
        assertEquals(before, record.save());
        record.promote(StarTier.ATTUNEMENT);
        assertFalse(required.evaluate(context).active());
        assertTrue(required.evaluate(GateContext.of(record)).active());
        assertTrue(
                gate("{\"any\":[{\"tier\":\"BRILLIANCE\"},{\"not\":{\"tier\":\"BRILLIANCE\"}}]}")
                        .evaluate(context)
                        .active());
        assertFalse(
                gate("{\"all\":[{\"tier\":\"DISCOVERY\"},{\"dimension\":\"minecraft:the_end\"}]}")
                        .evaluate(context)
                        .active());
    }

    @Test
    void externalStagesAreOptionalButTagsAreExplicit() {
        assertTrue(
                gate("{\"stage\":\"chapter\"}")
                        .evaluate(GateContext.of(new StarRecord()))
                        .active());
        assertFalse(
                gate("{\"tag\":\"secret\"}").evaluate(GateContext.of(new StarRecord())).active());
    }

    @Test
    void gateCodecRejectsUnknownMalformedOrDeepExpressions() {
        for (String json :
                List.of(
                        "{\"tier\":\"NOT_A_TIER\"}",
                        "{\"invented\":true}",
                        "{\"all\":[]}",
                        "{\"dimension\":\"Bad Identifier\"}"))
            assertTrue(
                    CodexGate.CODEC
                            .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                            .error()
                            .isPresent());
        String deep = "{}";
        for (int i = 0; i < 20; i++) deep = "{\"not\":" + deep + "}";
        assertTrue(
                CodexGate.CODEC
                        .parse(JsonOps.INSTANCE, JsonParser.parseString(deep))
                        .error()
                        .isPresent());
    }

    @Test
    void everyDeclaredPageRoundTripsAndAllKindsArePresent() {
        KnowledgeCatalog.initialize();
        var pages = KnowledgeCatalog.PAGES.defaults();
        assertTrue(pages.size() >= 300);
        assertEquals(
                Set.of(PageKind.values()),
                pages.values().stream()
                        .map(CodexPage::kind)
                        .collect(java.util.stream.Collectors.toSet()));
        pages.forEach(
                (id, page) ->
                        assertEquals(
                                page,
                                CodexPage.CODEC
                                        .parse(JsonOps.INSTANCE, page.json())
                                        .getOrThrow(false, s -> fail(s))));
        KnowledgeCatalog.NODES
                .all()
                .forEach(n -> n.pages().forEach(id -> assertTrue(pages.containsKey(id))));
    }

    @Test
    void invalidPagesFailLocallyInsteadOfThrowing() {
        for (String json :
                List.of(
                        "{\"kind\":\"unsupported\"}",
                        "{\"kind\":\"structure\"}",
                        "{\"kind\":\"text\",\"preview_shift\":[0,1]}"))
            assertTrue(
                    CodexPage.CODEC
                            .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                            .error()
                            .isPresent());
        var registry = new CodexRegistry();
        for (int i = 0; i < 10; i++) assertTrue(registry.find(id("absent")).isEmpty());
    }

    @Test
    void coordinatesIdsCyclesAndMissingPrerequisitesAreRejected() {
        var registry = new StudyNodeRegistry();
        var a = node("a", 0, List.of());
        registry.register(a);
        assertThrows(
                IllegalArgumentException.class, () -> registry.register(node("b", 0, List.of())));
        var cyclic = new StudyNodeRegistry();
        cyclic.register(node("a", 0, List.of(id("b"))));
        cyclic.register(node("b", 36, List.of(id("a"))));
        assertThrows(IllegalArgumentException.class, cyclic::validate);
        var missing = new StudyNodeRegistry();
        missing.register(node("a", 0, List.of(id("missing"))));
        assertThrows(IllegalArgumentException.class, missing::validate);
    }

    static StudyNode node(String name, int x, List<ResourceLocation> requires) {
        return new StudyNode(
                id(name),
                StudyBranch.DISCOVERY,
                x,
                0,
                id("icon"),
                requires,
                List.of(),
                CodexGate.any(),
                false);
    }

    @Test
    void seededShardChoiceIsIndependentOfOrderLocaleAndExternalRandoms() {
        var a = shard("a", CodexGate.any());
        var b = shard("b", CodexGate.any());
        var pool = List.of(a, b);
        for (long seed = 0; seed < 100; seed++)
            assertEquals(ShardPool.resolve(seed, pool), ShardPool.resolve(seed, List.of(b, a)));
        assertTrue(ShardPool.resolve(1, List.of()).isEmpty());
        assertEquals(a.moonPhases(7), a.moonPhases(7));
        assertEquals(new HashSet<>(a.moonPhases(7)).size(), a.moonPhases(7).size());
    }

    @Test
    void lockedOrIncompleteShardsNeverEnterThePool() {
        var pool = new ShardPool();
        pool.register(shard("a", CodexGate.any()));
        pool.register(shard("b", CodexGate.tier(StarTier.BRILLIANCE)));
        assertEquals(1, pool.eligible(GateContext.of(new StarRecord()), key -> true).size());
        assertTrue(
                pool.eligible(GateContext.of(new StarRecord()), key -> !key.equals("a.body"))
                        .isEmpty());
        assertThrows(
                IllegalArgumentException.class, () -> pool.register(shard("a", CodexGate.any())));
        KnowledgeCatalog.initialize();
        assertEquals(110, KnowledgeCatalog.SHARDS.all().size());
    }

    static LoreShard shard(String name, CodexGate gate) {
        return new LoreShard(
                id(name),
                name + ".name",
                name + ".ribbon",
                name + ".body",
                gate,
                gate,
                "LORE_INDEX",
                "");
    }

    @Test
    void milestoneWhitelistUsesPositiveMembershipAndValidatesStrings() {
        var criteria =
                MilestoneCriteria.parse(
                        JsonParser.parseString(
                                        "{\"signs\":[\"stellaeomphalos:aevitas\"],\"category\":\"major\"}")
                                .getAsJsonObject());
        assertTrue(criteria.matches(id("aevitas"), "major", 1, "DISCOVERY"));
        assertFalse(criteria.matches(id("armara"), "major", 1, "DISCOVERY"));
        assertFalse(criteria.matches(id("aevitas"), "weak", 1, "DISCOVERY"));
        assertThrows(
                RuntimeException.class,
                () ->
                        MilestoneCriteria.parse(
                                JsonParser.parseString("{\"signs\":[5]}").getAsJsonObject()));
    }

    @Test
    void invalidHandIsRejectedByWireCodec() {
        assertTrue(
                PktRevealShard.CODEC
                        .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"hand\":2}"))
                        .error()
                        .isPresent());
        assertTrue(
                PktRevealShard.CODEC
                        .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"hand\":-1}"))
                        .error()
                        .isPresent());
    }

    @Test
    void searchNeverIndexesLockedTextAndUnreadFilteringIsStable() {
        KnowledgeCatalog.initialize();
        var index = new CodexSearchIndex();
        var view = new StarRecord();
        index.build(
                KnowledgeCatalog.NODES,
                KnowledgeCatalog.PAGES.defaults(),
                GateContext.of(view),
                s -> KnowledgeCatalog.languages().getOrDefault(s, List.of(s)).get(0),
                ResourceLocation::toString);
        assertTrue(index.search("", "RADIANCE", false, Map.of()).isEmpty());
        var results = index.search("", "", false, Map.of());
        assertFalse(results.isEmpty());
        var page = results.get(0).page();
        assertTrue(
                index.search("", "", true, Map.of(page, 1)).stream()
                        .noneMatch(result -> result.page().equals(page)));
    }

    @Test
    void milestoneMinimumTierAcceptsHigherTiersButExactTierDoesNot() {
        var minimumJson = new JsonObject();
        minimumJson.addProperty("min_tier", "ATTUNEMENT");
        var minimum = MilestoneCriteria.parse(minimumJson);
        assertTrue(minimum.matches(id("rite"), "", 1, "RADIANCE"));
        assertFalse(minimum.matches(id("rite"), "", 1, "DISCOVERY"));
        var exactJson = new JsonObject();
        exactJson.addProperty("tier", "ATTUNEMENT");
        var exact = MilestoneCriteria.parse(exactJson);
        assertFalse(exact.matches(id("altar"), "", 1, "RADIANCE"));
    }
}
