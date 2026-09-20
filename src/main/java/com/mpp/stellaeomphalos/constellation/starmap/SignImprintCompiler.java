package com.mpp.stellaeomphalos.constellation.starmap;

import com.mpp.stellaeomphalos.constellation.sign.SignRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Compiles drawn strokes into a sign -> proportion table (plan 2.2.7).
 *
 * Coverage algorithm: strokes are processed in reverse draw order on an internal copy (the
 * caller's list is never mutated); each stroke occupies a DRAW_SIZE^2 rectangle; overlap with
 * earlier-processed rectangles is accumulated with second-order intersection dedup (pairwise
 * intersections of the intersections are subtracted); coverage = 1 - overlap / DRAW_SIZE^2,
 * clamped to [0,1]; multiple strokes of one sign are averaged; the average is then passed
 * through shiftDistribution.
 */
public final class SignImprintCompiler {
    private SignImprintCompiler() {}

    public static SignImprint compile(List<SignDrawn> drawn) {
        var proportions = coverages(drawn);
        var points = new LinkedHashMap<ResourceLocation, List<StarPoint>>();
        for (var stroke : drawn) {
            var sign = SignRegistry.all().stream().filter(candidate -> candidate.id().equals(stroke.sign())).findFirst();
            sign.ifPresent(found -> points.computeIfAbsent(stroke.sign(), key -> new ArrayList<>())
                    .addAll(found.stars().stream()
                            .map(star -> new StarPoint(star.x() + stroke.gridX(), star.y() + stroke.gridZ())).toList()));
        }
        var immutablePoints = new LinkedHashMap<ResourceLocation, List<StarPoint>>();
        points.forEach((sign, list) -> immutablePoints.put(sign, List.copyOf(list)));
        return new SignImprint(proportions, immutablePoints);
    }

    /** Pure coverage table after averaging and shiftDistribution; does not touch caller data. */
    public static Map<ResourceLocation, Double> coverages(List<SignDrawn> drawn) {
        var copy = new ArrayList<>(drawn);
        Collections.reverse(copy);
        int size = SignDrawn.DRAW_SIZE;
        var placed = new ArrayList<Rect>();
        var sums = new LinkedHashMap<ResourceLocation, Double>();
        var counts = new LinkedHashMap<ResourceLocation, Integer>();
        for (var stroke : copy) {
            var rect = Rect.square(stroke.gridX(), stroke.gridZ(), size);
            var intersections = new ArrayList<Rect>();
            double overlap = 0;
            for (var previous : placed) {
                var intersection = rect.intersect(previous);
                if (intersection != null) {
                    overlap += intersection.area();
                    intersections.add(intersection);
                }
            }
            for (int i = 0; i < intersections.size(); i++)
                for (int j = i + 1; j < intersections.size(); j++) {
                    var secondOrder = intersections.get(i).intersect(intersections.get(j));
                    if (secondOrder != null) overlap -= secondOrder.area();
                }
            double coverage = 1.0 - overlap / (size * size);
            coverage = Math.max(0.0, Math.min(1.0, coverage));
            sums.merge(stroke.sign(), coverage, Double::sum);
            counts.merge(stroke.sign(), 1, Integer::sum);
            placed.add(rect);
        }
        var result = new LinkedHashMap<ResourceLocation, Double>();
        sums.forEach((sign, sum) -> result.put(sign, shiftDistribution(sum / counts.get(sign))));
        return result;
    }

    /** Two-segment mapping that amplifies mid-range differences (plan 2.2.7 step 5, literal). */
    public static double shiftDistribution(double p) {
        double t = p >= 0.75 ? 3 * p - 2.25 : (4.0 / 3.0) * p - 1;
        double squared = 1 - t * t;
        return squared <= 0 ? 0 : Math.sqrt(squared);
    }

    private record Rect(int x0, int z0, int x1, int z1) {
        static Rect square(int x, int z, int size) { return new Rect(x, z, x + size, z + size); }
        int area() { return (x1 - x0) * (z1 - z0); }
        Rect intersect(Rect other) {
            int nx0 = Math.max(x0, other.x0);
            int nz0 = Math.max(z0, other.z0);
            int nx1 = Math.min(x1, other.x1);
            int nz1 = Math.min(z1, other.z1);
            return nx1 <= nx0 || nz1 <= nz0 ? null : new Rect(nx0, nz0, nx1, nz1);
        }
    }
}
