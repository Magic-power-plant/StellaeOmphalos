package com.mpp.stellaeomphalos.constellation.attribute;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

/** 晶体四项数值（尺寸/纯度/采集力/裂痕）+ 上限覆盖；随机生成为两次取均值的三角分布；
 *  研磨：基准损耗 + 纯度概率 + 连续失败翻倍；size ≤ 0 返回 null 表销毁。 */
public final class CrystalAttunement {

    public static final int DEFAULT_MAX_SIZE = 400;
    public static final int DEFAULT_MAX_PURITY = 100;
    public static final int DEFAULT_MAX_COLLECT = 100;

    private static final int GRIND_BASE_LOSS_MIN = 6;
    private static final int GRIND_BASE_LOSS_SPREAD = 7;
    private static final int MAX_FAILURE_SHIFT = 5;

    private final int size;
    private final int purity;
    private final int collect;
    private final int fracture;
    private final int maxSize;
    private final int maxPurity;
    private final int maxCollect;

    public CrystalAttunement(int size, int purity, int collect, int fracture) {
        this(size, purity, collect, fracture, DEFAULT_MAX_SIZE, DEFAULT_MAX_PURITY, DEFAULT_MAX_COLLECT);
    }

    public CrystalAttunement(int size, int purity, int collect, int fracture, int maxSize, int maxPurity, int maxCollect) {
        if (maxSize <= 0 || maxPurity <= 0 || maxCollect <= 0) throw new IllegalArgumentException("nonpositive cap");
        this.size = size;
        this.purity = purity;
        this.collect = collect;
        this.fracture = fracture;
        this.maxSize = maxSize;
        this.maxPurity = maxPurity;
        this.maxCollect = maxCollect;
    }

    public int size() {
        return size;
    }

    public int purity() {
        return purity;
    }

    public int collect() {
        return collect;
    }

    public int fracture() {
        return fracture;
    }

    public int maxSize() {
        return maxSize;
    }

    public int maxPurity() {
        return maxPurity;
    }

    public int maxCollect() {
        return maxCollect;
    }

    public static CrystalAttunement random(RandomSource random) {
        return random(random, DEFAULT_MAX_SIZE);
    }

    public static CrystalAttunement random(RandomSource random, int maxSize) {
        return new CrystalAttunement(triangular(random, maxSize), triangular(random, DEFAULT_MAX_PURITY),
                triangular(random, DEFAULT_MAX_COLLECT), 0, maxSize, DEFAULT_MAX_PURITY, DEFAULT_MAX_COLLECT);
    }

    static int triangular(RandomSource random, int bound) {
        return (random.nextInt(bound + 1) + random.nextInt(bound + 1)) / 2;
    }

    /** 研磨：成功（纯度概率）只付基准损耗并提升纯度/采集力；失败损耗随连续失败次数翻倍且累积裂痕。 */
    @Nullable
    public CrystalAttunement grind(RandomSource random, int consecutiveFailures) {
        if (consecutiveFailures < 0) throw new IllegalArgumentException("negative failure streak");
        int baseLoss = GRIND_BASE_LOSS_MIN + random.nextInt(GRIND_BASE_LOSS_SPREAD);
        boolean success = random.nextInt(maxPurity) < purity;
        int loss = success ? baseLoss : baseLoss << Math.min(consecutiveFailures, MAX_FAILURE_SHIFT);
        int newSize = size - loss;
        if (newSize <= 0) return null;
        if (!success) return new CrystalAttunement(newSize, purity, collect, fracture + 1, maxSize, maxPurity, maxCollect);
        int newPurity = Math.min(maxPurity, purity + 1 + random.nextInt(3));
        int newCollect = Math.min(maxCollect, collect + random.nextInt(2));
        return new CrystalAttunement(newSize, newPurity, newCollect, fracture, maxSize, maxPurity, maxCollect);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Size", size);
        tag.putInt("Purity", purity);
        tag.putInt("Collect", collect);
        tag.putInt("Fracture", fracture);
        tag.putInt("MaxSize", maxSize);
        tag.putInt("MaxPurity", maxPurity);
        tag.putInt("MaxCollect", maxCollect);
        return tag;
    }

    public static CrystalAttunement load(CompoundTag tag) {
        return new CrystalAttunement(tag.getInt("Size"), tag.getInt("Purity"), tag.getInt("Collect"),
                tag.getInt("Fracture"), tag.contains("MaxSize") ? tag.getInt("MaxSize") : DEFAULT_MAX_SIZE,
                tag.contains("MaxPurity") ? tag.getInt("MaxPurity") : DEFAULT_MAX_PURITY,
                tag.contains("MaxCollect") ? tag.getInt("MaxCollect") : DEFAULT_MAX_COLLECT);
    }
}
