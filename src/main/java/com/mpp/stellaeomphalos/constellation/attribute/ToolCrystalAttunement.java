package com.mpp.stellaeomphalos.constellation.attribute;

import java.util.List;
import javax.annotation.Nullable;

/** 工具多晶融合：尺寸求和，纯度与采集力取均值，裂痕取最大；效率 sqrt(collect/100) 带 0.05 下限。 */
public final class ToolCrystalAttunement {

    private final int size;
    private final double purity;
    private final double collect;
    private final int fracture;

    public ToolCrystalAttunement(int size, double purity, double collect, int fracture) {
        this.size = size;
        this.purity = purity;
        this.collect = collect;
        this.fracture = fracture;
    }

    public static ToolCrystalAttunement fuse(List<CrystalAttunement> crystals) {
        if (crystals == null || crystals.isEmpty()) throw new IllegalArgumentException("empty crystal list");
        int size = 0;
        double purity = 0.0;
        double collect = 0.0;
        int fracture = 0;
        for (var crystal : crystals) {
            size += crystal.size();
            purity += crystal.purity();
            collect += crystal.collect();
            fracture = Math.max(fracture, crystal.fracture());
        }
        int count = crystals.size();
        return new ToolCrystalAttunement(size, purity / count, collect / count, fracture);
    }

    public int size() {
        return size;
    }

    public double purity() {
        return purity;
    }

    public double collect() {
        return collect;
    }

    public int fracture() {
        return fracture;
    }

    public double efficiency() {
        return Math.max(0.05, Math.sqrt(collect / 100.0));
    }

    /** 研磨受损后的副本：尺寸扣除损耗；size ≤ 0 返回 null 表销毁。 */
    @Nullable
    public ToolCrystalAttunement copyDamagedCutting(int lostSize) {
        int newSize = size - lostSize;
        if (newSize <= 0) return null;
        return new ToolCrystalAttunement(newSize, purity, collect, fracture);
    }
}
