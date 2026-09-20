package com.mpp.stellaeomphalos.constellation.sign;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SignSkySchedulerTest {
    private static SignSkyScheduler scheduler(long seed) {
        return new SignSkyScheduler(seed, SignTestSupport.distributed(), SignTestSupport.traits(), List.of());
    }

    /** AC-2.9: the 8-day table is a pure function of the world seed. */
    @Test void eightDayTableIsDeterministicForFixedSeed() {
        assertEquals(scheduler(12345L).tableSnapshot(), scheduler(12345L).tableSnapshot());
        assertNotEquals(scheduler(12345L).tableSnapshot(), scheduler(12346L).tableSnapshot());
    }

    @Test void distributedSignsOccupyFiveConsecutiveDays() {
        for (long seed = 1; seed <= 64; seed++) {
            var scheduler = scheduler(seed);
            for (var sign : SignTestSupport.distributed()) {
                int start = windowStart(scheduler, sign);
                assertTrue(start >= 0, "sign " + sign.id() + " not placed for seed " + seed);
                for (int offset = 0; offset < SignSkyScheduler.WINDOW_DAYS; offset++) {
                    int day = (start + offset) % SignSkyScheduler.CYCLE_DAYS;
                    assertTrue(scheduler.tableDay(day).stream().anyMatch(entry -> entry.sign().equals(sign)),
                            "sign " + sign.id() + " seed " + seed + " missing on day " + day);
                }
            }
        }
    }

    /** Bitmap traversal fix: window starts must spread over all 8 cycle slots, not pile at one index. */
    @Test void windowStartsDistributeAcrossCycleSlots() {
        var histogram = new int[SignSkyScheduler.CYCLE_DAYS];
        int signs = SignTestSupport.distributed().size();
        int seeds = 256;
        for (long seed = 0; seed < seeds; seed++) {
            var scheduler = scheduler(seed);
            for (var sign : SignTestSupport.distributed()) {
                int start = windowStart(scheduler, sign);
                assertTrue(start >= 0, "sign " + sign.id() + " not placed for seed " + seed);
                histogram[start]++;
            }
        }
        int expected = seeds * signs / SignSkyScheduler.CYCLE_DAYS;
        for (int slot = 0; slot < histogram.length; slot++) {
            assertTrue(histogram[slot] > expected / 2,
                    "slot " + slot + " starved: " + histogram[slot] + " of " + expected + " expected");
            assertTrue(histogram[slot] < expected * 2,
                    "slot " + slot + " overloaded: " + histogram[slot] + " of " + expected + " expected");
        }
    }

    private static int windowStart(SignSkyScheduler scheduler, Sign sign) {
        boolean[] present = new boolean[SignSkyScheduler.CYCLE_DAYS];
        for (int day = 0; day < present.length; day++)
            present[day] = scheduler.tableDay(day).stream().anyMatch(entry -> entry.sign().equals(sign));
        for (int day = 0; day < present.length; day++)
            if (present[day] && !present[(day + present.length - 1) % present.length]) return day;
        return -1;
    }

    @Test void traitSignsOccupyBoundPhaseDaysAtZeroDistribution() {
        var scheduler = scheduler(7L);
        for (var trait : SignTestSupport.traits()) {
            for (int day = 0; day < SignSkyScheduler.CYCLE_DAYS; day++) {
                boolean bound = trait.showupMoonPhases(7L).contains(MoonPhase.values()[day]);
                boolean present = scheduler.tableDay(day).stream().anyMatch(entry -> entry.sign().equals(trait));
                assertEquals(bound, present, "trait " + trait.id() + " day " + day);
            }
            scheduler.setDay(trait.showupMoonPhases(7L).iterator().next().ordinal(), 0);
            assertEquals(0.0F, scheduler.distribution(trait));
            assertTrue(scheduler.byMoonPhase(trait.showupMoonPhases(7L).iterator().next()).contains(trait));
        }
    }

    @Test void majorDistributionIsConstantAndRitualFollowsSineCurve() {
        var scheduler = scheduler(42L);
        for (var sign : SignTestSupport.distributed()) {
            int start = windowStart(scheduler, sign);
            for (int offset = 0; offset < SignSkyScheduler.WINDOW_DAYS; offset++) {
                int day = (start + offset) % SignSkyScheduler.CYCLE_DAYS;
                var entry = scheduler.tableDay(day).stream().filter(e -> e.sign().equals(sign)).findFirst().orElseThrow();
                if (sign instanceof MajorSign) assertEquals(1.0F, entry.dist());
                else {
                    float expected = (float) (Math.sin(offset / 4.0 * Math.PI) * 0.25 + 0.75);
                    assertEquals(expected, entry.dist(), 0.0001F);
                    assertTrue(entry.dist() >= 0.75F && entry.dist() <= 1.0F);
                }
            }
        }
    }

    @Test void dailyActiveSetIsCappedAtTen() {
        for (long seed = 0; seed < 32; seed++) {
            var scheduler = scheduler(seed);
            for (int day = 0; day < SignSkyScheduler.CYCLE_DAYS; day++) {
                scheduler.setDay(day, 0);
                assertTrue(scheduler.activeSigns().size() <= SignSkyScheduler.DAILY_ACTIVE_CAP);
            }
        }
    }

    @Test void moonPhaseOrdinalOrderMatchesVanillaCycle() {
        assertEquals("FULL,WANING_3_4,WANING_1_2,WANING_1_4,NEW,WAXING_1_4,WAXING_1_2,WAXING_3_4",
                String.join(",", java.util.Arrays.stream(MoonPhase.values()).map(Enum::name).toList()));
        assertEquals(MoonPhase.FULL, MoonPhase.ofDay(0));
        assertEquals(MoonPhase.NEW, MoonPhase.ofDay(4));
        assertEquals(MoonPhase.FULL, MoonPhase.ofDay(8));
        assertEquals(MoonPhase.WAXING_3_4, MoonPhase.ofDay(-1));
    }

    @Test void solarEclipseWindowBoundaries() {
        var scheduler = scheduler(99L);
        long day = scheduler.solarCycleOffset();
        long center = SignSkyScheduler.DEFAULT_DAY_LENGTH / 4;
        long half = SignSkyScheduler.DEFAULT_DAY_LENGTH / 10;
        assertEquals(CelestialOmen.SOLAR_ECLIPSE, scheduler.omenAt(day, center).orElseThrow());
        assertEquals(CelestialOmen.SOLAR_ECLIPSE, scheduler.omenAt(day, center - half).orElseThrow());
        assertEquals(CelestialOmen.SOLAR_ECLIPSE, scheduler.omenAt(day, center + half).orElseThrow());
        assertTrue(scheduler.omenAt(day, center - half - 1).isEmpty() || day == scheduler.lunarCycleOffset());
        assertTrue(scheduler.omenAt(day, center + half + 1).isEmpty() || day == scheduler.lunarCycleOffset());
        assertTrue(scheduler.omenAt(day + 1, center).isEmpty()
                || (day + 1) % SignSkyScheduler.SOLAR_PERIOD == scheduler.solarCycleOffset()
                || (day + 1) % SignSkyScheduler.LUNAR_PERIOD == scheduler.lunarCycleOffset());
        assertTrue(scheduler.omensToday(day).contains(CelestialOmen.SOLAR_ECLIPSE));
    }

    @Test void lunarEclipseWindowBoundaries() {
        var scheduler = scheduler(99L);
        long day = scheduler.lunarCycleOffset();
        long center = SignSkyScheduler.DEFAULT_DAY_LENGTH * 3 / 4;
        long half = SignSkyScheduler.DEFAULT_DAY_LENGTH / 10;
        assertEquals(CelestialOmen.LUNAR_ECLIPSE, scheduler.omenAt(day, center).orElseThrow());
        assertEquals(CelestialOmen.LUNAR_ECLIPSE, scheduler.omenAt(day, center - half).orElseThrow());
        assertEquals(CelestialOmen.LUNAR_ECLIPSE, scheduler.omenAt(day, center + half).orElseThrow());
        assertTrue(scheduler.omenAt(day, center + half + 1).isEmpty() || day == scheduler.solarCycleOffset());
        assertTrue(scheduler.omensToday(day).contains(CelestialOmen.LUNAR_ECLIPSE));
    }

    @Test void randomStrongestPicksFromThousandthsMaximumGroup() {
        var scheduler = scheduler(2024L);
        var random = new Random(5L);
        for (int day = 0; day < SignSkyScheduler.CYCLE_DAYS; day++) {
            scheduler.setDay(day, 13000);
            int best = scheduler.activeSigns().stream().mapToInt(sign -> (int) (scheduler.distribution(sign) * 1000)).max().orElse(-1);
            if (best < 0) continue;
            for (int i = 0; i < 8; i++) {
                var pick = scheduler.randomStrongest(random, sign -> true);
                assertNotNull(pick);
                assertEquals(best, (int) (scheduler.distribution(pick) * 1000));
            }
            var none = scheduler.randomStrongest(random, sign -> false);
            assertNull(none);
        }
    }
}
