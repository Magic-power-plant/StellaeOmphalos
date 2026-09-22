package com.mpp.stellaeomphalos.content.blockentity.lumen;

import com.mpp.stellaeomphalos.OmphalosConfig;
import com.mpp.stellaeomphalos.core.util.math.SkyDensityField;
import com.mpp.stellaeomphalos.lumen.capability.LumenIO;
import com.mpp.stellaeomphalos.lumen.capability.LumenSource;
import com.mpp.stellaeomphalos.lumen.transport.LumenDistributionBridge;
import com.mpp.stellaeomphalos.lumen.transport.LumenMath;
import com.mpp.stellaeomphalos.lumen.transport.LumenNetworks;
import com.mpp.stellaeomphalos.lumen.transport.LumenSourceData;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Source end: holds LumenSourceData, computes per-tick output (sky gate, sign distribution, sky
 * noise, proximity penalty) and pushes it into the network. Sources without an owning sign use the
 * neutral distribution channel; signed sources read the constellation bridge (0 until registered).
 */
public abstract class LumenSourceBlockEntity extends SkyboundLumenBlockEntity implements LumenSource {
    private final ResourceLocation providerId;
    private LumenSourceData data;
    private double noise = -1;

    protected LumenSourceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, ResourceLocation providerId, long baseOutput) {
        super(type, pos, state);
        this.providerId = providerId;
        data = new LumenSourceData(providerId, baseOutput, Optional.empty(), true, false, false, 1, 0);
    }

    @Override public final LumenIO io() { return LumenIO.SOURCE; }
    @Override public final LumenSourceData data() { return data; }

    @Override
    public long provideLumen(Level level, long gameTick) {
        double distribution = data.sign()
                .map(sign -> LumenDistributionBridge.distribution(level, sign))
                .orElseGet(LumenDistributionBridge::neutral);
        double proximity = level instanceof ServerLevel server
                ? LumenNetworks.of(server).proximityFactorAt(worldPosition)
                : data.proximityFactor();
        return LumenMath.sourceOutput(data.baseOutput(), data.seesSky(), distribution, data.enhanced(), noise(level), proximity);
    }

    public final void attune(Optional<ResourceLocation> sign) {
        data = new LumenSourceData(providerId, data.baseOutput(), sign, data.autoLink(), data.seesSky(),
                data.enhanced(), data.proximityFactor(), data.noiseFactor());
        markClientDirty();
        onNeighborChanged(level, worldPosition);
    }

    private double noise(Level level) {
        if (noise < 0) {
            long seed = level instanceof ServerLevel server ? server.getSeed() : 0;
            int grid = OmphalosConfig.COMMON.integer("performance.skyDensityGridSize");
            noise = new SkyDensityField(seed, grid).sample(worldPosition.getX(), worldPosition.getZ());
        }
        return noise;
    }

    @Override
    protected void tickSkybound(boolean skyVisible) {
        if (skyVisible != data.seesSky()) { data = data.withSeesSky(skyVisible); markClientDirty(); }
        if (!(level instanceof ServerLevel server) || !OmphalosConfig.SERVER.flag("gameplay.lumenEnabled")) return;
        if (provideLumen(server, server.getGameTime()) <= 0) return;
        var network = LumenNetworks.of(server);
        long budget = OmphalosConfig.COMMON.integer("performance.lumenRoutingStepsPerTick");
        for (var delivery : network.resolve(worldPosition, budget)) network.deliver(delivery);
    }

    @Override protected void writeClientState(CompoundTag tag) {
        tag.putBoolean("SeesSky", data.seesSky());tag.putBoolean("Enhanced",data.enhanced());
        data.sign().ifPresent(sign -> tag.putString("Sign",sign.toString()));
    }
    @Override protected void readClientState(CompoundTag tag) {
        var sign=Optional.ofNullable(net.minecraft.resources.ResourceLocation.tryParse(tag.getString("Sign")));
        data=new LumenSourceData(providerId,data.baseOutput(),sign,data.autoLink(),tag.getBoolean("SeesSky"),tag.getBoolean("Enhanced"),data.proximityFactor(),data.noiseFactor());
    }

    @Override
    protected void writePersistent(CompoundTag tag) {
        super.writePersistent(tag);
        tag.putBoolean("AutoLink", data.autoLink());
        tag.putBoolean("Enhanced", data.enhanced());
        data.sign().ifPresent(sign -> tag.putString("Sign", sign.toString()));
    }

    @Override
    protected void readPersistent(CompoundTag tag) {
        super.readPersistent(tag);
        Optional<ResourceLocation> sign = tag.contains("Sign")
                ? Optional.ofNullable(ResourceLocation.tryParse(tag.getString("Sign")))
                : Optional.empty();
        data = new LumenSourceData(providerId, data.baseOutput(), sign, tag.getBoolean("AutoLink"),
                data.seesSky(), tag.getBoolean("Enhanced"), data.proximityFactor(), data.noiseFactor());
    }
}
