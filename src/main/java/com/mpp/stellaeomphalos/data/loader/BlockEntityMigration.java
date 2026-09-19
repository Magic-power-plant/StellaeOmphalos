package com.mpp.stellaeomphalos.data.loader;

import net.minecraft.nbt.CompoundTag;

public final class BlockEntityMigration {
    private BlockEntityMigration() {}
    public static CompoundTag upgrade(CompoundTag blockEntityData, String payloadKey, MigrationChain chain, long tick) {
        var output = blockEntityData.copy();
        output.put(payloadKey, chain.apply(blockEntityData.getCompound(payloadKey), tick));
        return output;
    }
}
