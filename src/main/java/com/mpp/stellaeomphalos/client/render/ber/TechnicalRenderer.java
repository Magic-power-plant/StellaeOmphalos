package com.mpp.stellaeomphalos.client.render.ber;

import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.content.block.TechnicalBlock;
import com.mpp.stellaeomphalos.content.blockentity.rite.TechnicalBlockEntity;

/** Existing technical blocks share one BE type; dispatch its four visual capabilities. */
public final class TechnicalRenderer extends OmphalosBlockEntityRenderer<TechnicalBlockEntity> {
    private final MachineRenderers.ObservatoryRenderer observatory =
            new MachineRenderers.ObservatoryRenderer();
    private final MachineRenderers.PhantomBlockEntityRenderer block =
            new MachineRenderers.PhantomBlockEntityRenderer();
    private final MachineRenderers.PhantomTreeRenderer tree =
            new MachineRenderers.PhantomTreeRenderer();
    private final MachineRenderers.GatewayRenderer gateway = new MachineRenderers.GatewayRenderer();

    public TechnicalRenderer() {
        super(64);
    }

    protected void renderModel(TechnicalBlockEntity b, WorldDraw d, int l, int o) {
        String kind = ((TechnicalBlock) b.getBlockState().getBlock()).kind();
        switch (kind) {
            case "observatory" -> observatory.renderModel(b, d, l, o);
            case "proxy_foliage" -> tree.renderModel(b, d, l, o);
            case "frame_shell", "mirage_shell" -> block.renderModel(b, d, l, o);
            case "gate_core" -> gateway.renderModel(b, d, l, o);
            default -> {}
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TechnicalBlockEntity b) {
        return ((TechnicalBlock) b.getBlockState().getBlock()).kind().equals("gate_core");
    }
}
