package com.mpp.stellaeomphalos.constellation.sign;

import com.mpp.stellaeomphalos.constellation.domain.DomainEffect;
import com.mpp.stellaeomphalos.constellation.domain.DomainOrigin;
import com.mpp.stellaeomphalos.constellation.starmap.StarLine;
import com.mpp.stellaeomphalos.constellation.starmap.StarPoint;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Mutable during data-driven assembly, {@link #seal()}ed before publication; identity is the id only. */
public abstract class AbstractSign implements Sign {
    private final ResourceLocation id;
    private final int renderColor;
    private final List<StarPoint> stars = new ArrayList<>();
    private final List<StarLine> lines = new ArrayList<>();
    private final List<ItemStack> signatureItems;
    private boolean sealed;

    protected AbstractSign(ResourceLocation id, int renderColor, List<ItemStack> signatureItems) {
        this.id = id;
        this.renderColor = renderColor;
        this.signatureItems = List.copyOf(signatureItems);
    }

    /** Adds a star; coordinates wrap modulo the grid and duplicates are ignored. */
    public AbstractSign addStar(int x, int y) {
        return addStar(new StarPoint(Math.floorMod(x, StarPoint.GRID), Math.floorMod(y, StarPoint.GRID)));
    }

    public AbstractSign addStar(StarPoint point) {
        assertWritable();
        if (!stars.contains(point)) stars.add(point);
        return this;
    }

    /** Adds an undirected connection; both endpoints must be known stars; self-links and duplicates rejected. */
    public AbstractSign addConnection(StarPoint a, StarPoint b) {
        assertWritable();
        if (a.equals(b)) throw new IllegalArgumentException("Self connection at " + a + " in " + id);
        if (!stars.contains(a) || !stars.contains(b))
            throw new IllegalArgumentException("Connection endpoint missing from star set in " + id);
        var line = new StarLine(a, b);
        if (!lines.contains(line)) lines.add(line);
        return this;
    }

    /** Irreversible; after sealing, structural mutation throws. */
    public AbstractSign seal() { sealed = true; return this; }
    private void assertWritable() { if (sealed) throw new IllegalStateException("Sign " + id + " is sealed"); }

    @Override public ResourceLocation id() { return id; }
    @Override public Component displayName() { return Component.translatable("sign." + id.getNamespace() + "." + id.getPath()); }
    @Override public int renderColor() { return renderColor; }
    @Override public List<StarPoint> stars() { return List.copyOf(stars); }
    @Override public List<StarLine> lines() { return List.copyOf(lines); }
    @Override public List<ItemStack> signatureItems() { return signatureItems; }
    @Override public boolean canDiscover(ServerPlayer player, SignDiscoveryView progress) { return true; }

    @Override public boolean equals(Object other) { return other instanceof Sign sign && sign.id().equals(id); }
    @Override public int hashCode() { return id.hashCode(); }
    @Override public String toString() { return id.toString(); }

    /** Major sign; the domain module binds its ritual effect after construction. */
    public static final class Major extends AbstractSign implements MajorSign {
        private volatile @Nullable DomainEffect ritualEffect;
        public Major(ResourceLocation id, int renderColor, List<ItemStack> signatureItems) { super(id, renderColor, signatureItems); }
        public void bindRitualEffect(@Nullable DomainEffect effect) { this.ritualEffect = effect; }
        @Override public @Nullable DomainEffect ritualEffect(DomainOrigin origin) { return ritualEffect; }
    }

    /** Ritual (minor) sign; the domain module binds its ritual effect after construction. */
    public static final class Ritual extends AbstractSign implements RitualSign {
        private volatile @Nullable DomainEffect ritualEffect;
        public Ritual(ResourceLocation id, int renderColor, List<ItemStack> signatureItems) { super(id, renderColor, signatureItems); }
        public void bindRitualEffect(@Nullable DomainEffect effect) { this.ritualEffect = effect; }
        @Override public @Nullable DomainEffect ritualEffect(DomainOrigin origin) { return ritualEffect; }
    }

    /** Anomalous sign bound to one celestial omen; shows up at full strength only while its omen is active. */
    public static final class Anomalous extends AbstractSign implements AnomalousSign {
        private final CelestialOmen omen;
        public Anomalous(ResourceLocation id, int renderColor, List<ItemStack> signatureItems, CelestialOmen omen) {
            super(id, renderColor, signatureItems);
            this.omen = omen;
        }
        public CelestialOmen boundOmen() { return omen; }
        @Override public boolean doesShowUp(SignSkyState sky, Level level, long day) {
            return sky.omen().filter(active -> active == omen).isPresent();
        }
        @Override public float distribution(SignSkyState sky, Level level, long day) {
            return doesShowUp(sky, level, day) ? 1.0F : 0.0F;
        }
    }

    /** Trait sign; bound to explicit moon phases, or deterministically derives two phases from the world seed. */
    public static final class Trait extends AbstractSign implements TraitSign {
        private final Set<MoonPhase> boundPhases;
        public Trait(ResourceLocation id, int renderColor, List<ItemStack> signatureItems, Set<MoonPhase> boundPhases) {
            super(id, renderColor, signatureItems);
            this.boundPhases = Set.copyOf(boundPhases);
        }
        @Override public Set<MoonPhase> showupMoonPhases(long worldSeed) {
            if (!boundPhases.isEmpty()) return boundPhases;
            var random = new Random(worldSeed ^ id().hashCode() * 0x9E3779B97F4A7C15L);
            var phases = new LinkedHashSet<MoonPhase>();
            while (phases.size() < 2) phases.add(MoonPhase.values()[random.nextInt(8)]);
            return Set.copyOf(phases);
        }
    }
}
