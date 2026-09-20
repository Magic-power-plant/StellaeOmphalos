package com.mpp.stellaeomphalos.constellation.sign;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * Per-dimensional deterministic sky schedule. The 8-day cycle table is a pure function of the
 * world seed: trait signs occupy their moon-phase days at constant zero distribution; major and
 * ritual signs each claim a 5-consecutive-day window found by scanning all 8 start slots of the
 * occupancy bitmap from a random start. Day state is recomputed on every day change, so time
 * rewinds simply rebuild the target day.
 */
public final class SignSkyScheduler implements SignSkyState {
    public static final int CYCLE_DAYS = 8;
    public static final int WINDOW_DAYS = 5;
    public static final int SLOTS_PER_DAY = 16;
    public static final int DAILY_ACTIVE_CAP = 10;
    public static final int SOLAR_PERIOD = 36;
    public static final int LUNAR_PERIOD = 68;
    public static final long DEFAULT_DAY_LENGTH = 24000L;

    /** One scheduled appearance: the sign and its distribution for that day. */
    public record Entry(Sign sign, float dist) { }

    private final long seed;
    private final long dayLength;
    private final List<TraitSign> traits;
    private final List<AnomalousSign> anomalous;
    private final int solarOffset;
    private final int lunarOffset;
    /** Slot-ordered major/ritual entries per cycle day; traits are appended separately. */
    private final List<Entry>[] slots;
    private final List<Entry>[] traitSlots;

    private long day = -1;
    private long dayTime;
    private List<Sign> active = List.of();

    @SuppressWarnings("unchecked")
    public SignSkyScheduler(long seed, List<Sign> distributed, List<TraitSign> traits, List<AnomalousSign> anomalous, long dayLength) {
        if (dayLength <= 0) throw new IllegalArgumentException("Nonpositive day length");
        this.seed = seed;
        this.dayLength = dayLength;
        this.traits = List.copyOf(traits);
        this.anomalous = List.copyOf(anomalous);
        this.slots = new List[CYCLE_DAYS];
        this.traitSlots = new List[CYCLE_DAYS];
        for (int i = 0; i < CYCLE_DAYS; i++) { slots[i] = new ArrayList<>(); traitSlots[i] = new ArrayList<>(); }
        var omenRandom = new Random(seed ^ 0x5DEECE66D94A2F61L);
        this.solarOffset = omenRandom.nextInt(SOLAR_PERIOD);
        this.lunarOffset = omenRandom.nextInt(LUNAR_PERIOD);
        generate(new Random(seed), List.copyOf(distributed));
    }

    public SignSkyScheduler(long seed, List<Sign> distributed, List<TraitSign> traits, List<AnomalousSign> anomalous) {
        this(seed, distributed, traits, anomalous, DEFAULT_DAY_LENGTH);
    }

    private void generate(Random random, List<Sign> distributed) {
        var occupied = new BitSet(CYCLE_DAYS * SLOTS_PER_DAY);
        for (var trait : traits) {
            for (var phase : trait.showupMoonPhases(seed))
                traitSlots[phase.ordinal()].add(new Entry(trait, 0.0F));
        }
        for (var sign : distributed) {
            boolean major = sign instanceof MajorSign;
            int start = random.nextInt(CYCLE_DAYS);
            // True traversal of all 8 start slots: pick the window whose fullest day stays emptiest;
            // ties resolve in scan order from the random start, which spreads windows uniformly.
            int placed = start;
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < CYCLE_DAYS; i++) {
                int candidate = (start + i) % CYCLE_DAYS;
                int score = 0;
                for (int offset = 0; offset < WINDOW_DAYS; offset++)
                    score = Math.max(score, dayLoad(occupied, (candidate + offset) % CYCLE_DAYS) + 1);
                if (score < bestScore) { bestScore = score; placed = candidate; }
            }
            for (int offset = 0; offset < WINDOW_DAYS; offset++) {
                int day = (placed + offset) % CYCLE_DAYS;
                int slot = firstFreeSlot(occupied, day);
                if (slot < 0) continue;                       // day full; drop this day's appearance
                occupied.set(day * SLOTS_PER_DAY + slot);
                float dist = major ? 1.0F : (float) (Math.sin(offset / 4.0 * Math.PI) * 0.25 + 0.75);
                slots[day].add(new Entry(sign, dist));
            }
        }
    }

    private static int dayLoad(BitSet occupied, int day) {
        int free = firstFreeSlot(occupied, day);
        return free < 0 ? SLOTS_PER_DAY : free;
    }

    /** Within-day index of the first free slot (equals the day's load), or -1 when the day is full. */
    private static int firstFreeSlot(BitSet occupied, int day) {
        int slot = occupied.nextClearBit(day * SLOTS_PER_DAY) - day * SLOTS_PER_DAY;
        return slot < SLOTS_PER_DAY ? slot : -1;
    }

    /**
     * Advances (or rewinds) to the given day; daily state is a pure function of the day index and
     * the active omen, so a rewind simply rebuilds the target day (equivalent to replaying from
     * currentDay+1). Rebuilds are skipped when neither the day nor the omen changed.
     */
    public void setDay(long day, long dayTime) {
        long time = Math.floorMod(dayTime, dayLength);
        if (this.day == day && !active.isEmpty() && omenAt(day, time).equals(omenAt(this.day, this.dayTime))) {
            this.dayTime = time;
            return;
        }
        this.day = day;
        this.dayTime = time;
        int cycleDay = (int) Math.floorMod(day, CYCLE_DAYS);
        var todays = new ArrayList<Sign>();
        slots[cycleDay].stream().limit(Math.min(DAILY_ACTIVE_CAP, slots[cycleDay].size())).forEach(entry -> todays.add(entry.sign()));
        traitSlots[cycleDay].forEach(entry -> { if (todays.size() < DAILY_ACTIVE_CAP) todays.add(entry.sign()); });
        for (var anomaly : anomalous) {
            if (!todays.contains(anomaly) && anomaly.doesShowUp(this, null, day)) todays.add(anomaly);
        }
        active = List.copyOf(todays);
    }

    @Override public List<Sign> activeSigns() { return active; }
    @Override public long day() { return day; }
    public long dayTime() { return dayTime; }
    public long skySeed() { return seed; }
    public long dayLength() { return dayLength; }
    public int solarCycleOffset() { return solarOffset; }
    public int lunarCycleOffset() { return lunarOffset; }

    @Override public float distribution(Sign sign) {
        int cycleDay = (int) Math.floorMod(day, CYCLE_DAYS);
        for (var entry : slots[cycleDay]) if (entry.sign().equals(sign)) return entry.dist();
        for (var entry : traitSlots[cycleDay]) if (entry.sign().equals(sign)) return 0.0F;
        if (sign instanceof AnomalousSign anomaly)
            return Math.max(0.0F, Math.min(1.0F, anomaly.distribution(this, null, day)));
        return 0.0F;
    }

    /** Trait signs bound to the given moon phase for this world seed. */
    public List<Sign> byMoonPhase(MoonPhase phase) {
        return traitSlots[phase.ordinal()].stream().<Sign>map(Entry::sign).toList();
    }

    @Override public Optional<CelestialOmen> omen() { return omenAt(day, dayTime); }

    /** The omen active at the given moment, if any; pure function of seed, day and time. */
    public Optional<CelestialOmen> omenAt(long day, long dayTime) {
        long time = Math.floorMod(dayTime, dayLength);
        if (Math.floorMod(day, SOLAR_PERIOD) == solarOffset && within(time, dayLength / 4, dayLength / 10))
            return Optional.of(CelestialOmen.SOLAR_ECLIPSE);
        if (Math.floorMod(day, LUNAR_PERIOD) == lunarOffset && within(time, dayLength * 3 / 4, dayLength / 10))
            return Optional.of(CelestialOmen.LUNAR_ECLIPSE);
        return Optional.empty();
    }

    /** All omens whose window touches the given day. */
    public List<CelestialOmen> omensToday(long day) {
        var result = new ArrayList<CelestialOmen>();
        if (Math.floorMod(day, SOLAR_PERIOD) == solarOffset) result.add(CelestialOmen.SOLAR_ECLIPSE);
        if (Math.floorMod(day, LUNAR_PERIOD) == lunarOffset) result.add(CelestialOmen.LUNAR_ECLIPSE);
        return List.copyOf(result);
    }

    private static boolean within(long time, long center, long halfWidth) {
        return time >= center - halfWidth && time <= center + halfWidth;
    }

    /** Picks uniformly among the strongest currently active signs passing the filter; ties broken randomly. */
    public @Nullable Sign randomStrongest(Random random, Predicate<Sign> filter) {
        int best = Integer.MIN_VALUE;
        var strongest = new ArrayList<Sign>();
        for (var sign : active) {
            if (!filter.test(sign)) continue;
            int value = (int) (distribution(sign) * 1000);
            if (value > best) { best = value; strongest.clear(); strongest.add(sign); }
            else if (value == best) strongest.add(sign);
        }
        return strongest.isEmpty() ? null : strongest.get(random.nextInt(strongest.size()));
    }

    /** Raw cycle table for a cycle day (major/ritual slots first, then trait placeholders). For tests/debug. */
    public List<Entry> tableDay(int cycleDay) {
        var day = Math.floorMod(cycleDay, CYCLE_DAYS);
        var result = new ArrayList<>(slots[day]);
        result.addAll(traitSlots[day]);
        return List.copyOf(result);
    }

    /** Snapshot string of the whole 8-day table; used by determinism tests and debug dumps. */
    public String tableSnapshot() {
        var builder = new StringBuilder();
        for (int d = 0; d < CYCLE_DAYS; d++) {
            builder.append(d).append(':');
            for (var entry : tableDay(d))
                builder.append(entry.sign().id().getPath()).append('@').append((int) (entry.dist() * 1000)).append(',');
            builder.append('\n');
        }
        return builder.toString();
    }
}
