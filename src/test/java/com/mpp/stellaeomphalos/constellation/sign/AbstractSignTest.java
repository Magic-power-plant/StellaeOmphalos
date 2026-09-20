package com.mpp.stellaeomphalos.constellation.sign;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class SignTestSupport {
    static ResourceLocation id(String name) { return new ResourceLocation("stellaeomphalos", name); }

    static AbstractSign.Major major(String name) {
        return (AbstractSign.Major) new AbstractSign.Major(id(name), 0xFFFFFF, List.of()).seal();
    }

    static AbstractSign.Ritual ritual(String name) {
        return (AbstractSign.Ritual) new AbstractSign.Ritual(id(name), 0xFFFFFF, List.of()).seal();
    }

    static AbstractSign.Trait trait(String name, MoonPhase... phases) {
        return (AbstractSign.Trait) new AbstractSign.Trait(id(name), 0xFFFFFF, List.of(), Set.of(phases)).seal();
    }

    static List<Sign> distributed() {
        return List.of(
                major("aevitas"), major("armara"), major("discidia"), major("evorsio"), major("vicio"),
                ritual("bootes"), ritual("fornax"), ritual("horologium"), ritual("lucerna"),
                ritual("mineralis"), ritual("octans"), ritual("pelotrio"));
    }

    static List<TraitSign> traits() {
        return List.of(
                trait("gelu", MoonPhase.NEW, MoonPhase.WANING_1_4),
                trait("ulteria", MoonPhase.WAXING_1_2, MoonPhase.FULL),
                trait("alcara", MoonPhase.WANING_3_4, MoonPhase.WANING_1_2),
                trait("vorux", MoonPhase.WAXING_1_4, MoonPhase.WAXING_3_4));
    }
}

class AbstractSignTest {
    @Test void addStarDeduplicatesAndWrapsModuloGrid() {
        var sign = new AbstractSign.Major(SignTestSupport.id("wrap_test"), 0, List.of());
        sign.addStar(5, 5).addStar(5, 5);
        assertEquals(1, sign.stars().size());
        sign.addStar(31 + 2, 7);   // wraps % 31 -> (2, 7)
        assertTrue(sign.stars().contains(new StarPoint(2, 7)));
    }

    @Test void addConnectionRejectsSelfDuplicateAndUnknownEndpoints() {
        var sign = new AbstractSign.Ritual(SignTestSupport.id("line_test"), 0, List.of());
        var a = new StarPoint(1, 1);
        var b = new StarPoint(2, 2);
        var c = new StarPoint(3, 3);
        sign.addStar(a).addStar(b);
        assertThrows(IllegalArgumentException.class, () -> sign.addConnection(a, a));
        assertThrows(IllegalArgumentException.class, () -> sign.addConnection(a, c));
        sign.addConnection(a, b).addConnection(b, a);   // undirected duplicate ignored
        assertEquals(1, sign.lines().size());
    }

    @Test void sealingBlocksStructuralMutation() {
        var sign = new AbstractSign.Major(SignTestSupport.id("seal_test"), 0, List.of());
        sign.addStar(1, 1);
        sign.seal();
        assertThrows(IllegalStateException.class, () -> sign.addStar(2, 2));
        assertThrows(IllegalStateException.class, () -> sign.addConnection(new StarPoint(1, 1), new StarPoint(2, 2)));
    }

    @Test void traitSignDerivesPhasesDeterministicallyFromSeed() {
        var trait = new AbstractSign.Trait(SignTestSupport.id("derived"), 0, List.of(), Set.of());
        var first = trait.showupMoonPhases(777L);
        var second = trait.showupMoonPhases(777L);
        assertEquals(first, second);
        assertEquals(2, first.size());
        var explicit = new AbstractSign.Trait(SignTestSupport.id("explicit"), 0, List.of(), Set.of(MoonPhase.FULL));
        assertEquals(Set.of(MoonPhase.FULL), explicit.showupMoonPhases(1L));
        assertEquals(Set.of(MoonPhase.FULL), explicit.showupMoonPhases(999999L));
    }
}
