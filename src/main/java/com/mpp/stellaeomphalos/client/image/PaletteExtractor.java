package com.mpp.stellaeomphalos.client.image;

import java.util.*;

/**
 * Weighted median cut over 5-bit RGB bins; fully deterministic and independent of graphics APIs.
 */
public final class PaletteExtractor {
    private PaletteExtractor() {}

    public static int[] palette(int[] argb, int maximum, int quality, boolean ignoreWhite) {
        if (maximum < 1 || maximum > 32 || quality < 1)
            throw new IllegalArgumentException("Invalid quantizer settings");
        var histogram = new TreeMap<Integer, Integer>();
        for (int i = 0; i < argb.length; i += quality) {
            int c = argb[i];
            if ((c >>> 24) < 128) continue;
            int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            if (ignoreWhite && r > 245 && g > 245 && b > 245) continue;
            int key = (r >> 3) << 10 | (g >> 3) << 5 | (b >> 3);
            histogram.merge(key, 1, Integer::sum);
        }
        if (histogram.isEmpty()) return new int[] {0xffb7dcff};
        var boxes = new ArrayList<List<Integer>>();
        boxes.add(new ArrayList<>(histogram.keySet()));
        while (boxes.size() < maximum) {
            List<Integer> chosen = null;
            int chosenRange = -1, axis = 0;
            for (var box : boxes)
                if (box.size() > 1)
                    for (int a = 0; a < 3; a++) {
                        int min = 31, max = 0;
                        for (int c : box) {
                            int v = (c >> (a * 5)) & 31;
                            min = Math.min(min, v);
                            max = Math.max(max, v);
                        }
                        if (max - min > chosenRange) {
                            chosenRange = max - min;
                            chosen = box;
                            axis = a;
                        }
                    }
            if (chosen == null) break;
            final int shift = axis * 5;
            chosen.sort(Comparator.comparingInt(c -> (c >> shift) & 31));
            int total = 0;
            for (int c : chosen) total += histogram.get(c);
            int count = 0, split = 1;
            for (; split < chosen.size(); split++) {
                count += histogram.get(chosen.get(split - 1));
                if (count >= total / 2) break;
            }
            split = Math.min(chosen.size() - 1, split);
            boxes.remove(chosen);
            boxes.add(new ArrayList<>(chosen.subList(0, split)));
            boxes.add(new ArrayList<>(chosen.subList(split, chosen.size())));
        }
        boxes.sort(
                Comparator.<List<Integer>>comparingInt(
                                box -> {
                                    int count = 0;
                                    for (int c : box) count += histogram.get(c);
                                    return count;
                                })
                        .reversed());
        int[] result = new int[boxes.size()];
        int index = 0;
        for (var box : boxes) {
            long red = 0, green = 0, blue = 0, count = 0;
            for (int c : box) {
                int n = histogram.get(c);
                count += n;
                red += ((c >> 10 & 31) * 8 + 4L) * n;
                green += ((c >> 5 & 31) * 8 + 4L) * n;
                blue += ((c & 31) * 8 + 4L) * n;
            }
            result[index++] =
                    0xff000000
                            | (int) (red / count) << 16
                            | (int) (green / count) << 8
                            | (int) (blue / count);
        }
        return result;
    }

    public static int dominantColor(int[] argb) {
        return palette(argb, 5, 1, true)[0];
    }
}
