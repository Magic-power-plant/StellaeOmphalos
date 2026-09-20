package com.mpp.stellaeomphalos.crafting.infusion;

import com.mpp.stellaeomphalos.crafting.altar.recipe.AbstractCraftTask;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public final class InfusionTask extends AbstractCraftTask {
    private List<BlockPos> positions = List.of();

    public InfusionTask(ResourceLocation id, long hash, int duration, UUID crafter) {
        super(id, hash, duration, crafter);
    }

    public List<BlockPos> solventPositions() {
        return positions;
    }

    public void solventPositions(List<BlockPos> positions) {
        this.positions = positions.stream().limit(12).map(BlockPos::immutable).distinct().toList();
    }

    @Override
    public CompoundTag save() {
        var tag = super.save();
        tag.putLongArray(
                "SolventPositions", positions.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public static Optional<InfusionTask> load(CompoundTag tag) {
        var id = ResourceLocation.tryParse(tag.getString("Recipe"));
        if (id == null) return Optional.empty();
        var result =
                new InfusionTask(
                        id,
                        tag.getLong("Hash"),
                        Math.max(1, tag.getInt("Total")),
                        tag.hasUUID("Crafter") ? tag.getUUID("Crafter") : new UUID(0, 0));
        result.restore(tag);
        result.solventPositions(
                Arrays.stream(tag.getLongArray("SolventPositions"))
                        .limit(12)
                        .mapToObj(BlockPos::of)
                        .toList());
        return Optional.of(result);
    }
}
