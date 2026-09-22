package com.mpp.stellaeomphalos.client.render.ber;

import com.mpp.stellaeomphalos.client.render.OmphalosRenderTypes.Kind;
import com.mpp.stellaeomphalos.client.render.model.MachineMeshes;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;
import com.mpp.stellaeomphalos.content.block.PartSixBlocks.MachineBlockEntity;
import com.mpp.stellaeomphalos.content.blockentity.crafting.*;
import com.mpp.stellaeomphalos.content.blockentity.lumen.LumenContent;
import com.mpp.stellaeomphalos.content.blockentity.rite.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Sixteen visual roles, including the three technical roles sharing one registered BE type. */
public final class MachineRenderers {
    private MachineRenderers() {}

    private static double time(BlockEntity be, WorldDraw d) {
        return be.getLevel().getGameTime() + d.partial;
    }

    private static void item(
            ItemStack stack, BlockEntity be, WorldDraw d, int light, int overlay, double y) {
        if (stack.isEmpty()) return;
        d.pose.pushPose();
        try {
            d.pose.translate(0, y, 0);
            d.pose.mulPose(d.rotate(0, (float) (time(be, d) * .025), 0));
            d.pose.scale(.625F, .625F, .625F);
            Minecraft.getInstance()
                    .getItemRenderer()
                    .renderStatic(
                            stack,
                            ItemDisplayContext.GROUND,
                            light,
                            overlay,
                            d.pose,
                            d.buffers,
                            be.getLevel(),
                            0);
        } finally {
            d.pose.popPose();
        }
    }

    public static void crystal(WorldDraw d, int color) {
        if (com.mpp.stellaeomphalos.client.render.obj.ObjMeshLibrary.drawCrystal(d, color)) return;
        var v = d.buffer(Kind.GLOW_LAYER);
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2, b = (i + 1) * Math.PI / 2;
            d.vertex(v, 0, .9, 0, color, .5F, 0);
            d.vertex(v, Math.cos(a) * .22, .4, Math.sin(a) * .22, color, 0, .5F);
            d.vertex(v, 0, .05, 0, color, .5F, 1);
            d.vertex(v, Math.cos(b) * .22, .4, Math.sin(b) * .22, color, 1, .5F);
        }
    }

    private static int lensTint(MachineBlockEntity be, int alpha) {
        var dye =
                net.minecraft.world.item.DyeColor.byName(
                        be.lensColor(), net.minecraft.world.item.DyeColor.LIGHT_BLUE);
        return (alpha << 24) | (dye.getTextColor() & 0xffffff);
    }

    private static void halo(WorldDraw d, double radius, int color) {
        var v = d.buffer(Kind.BEAM_ADDITIVE);
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16, b = (i + 1) * Math.PI / 16;
            d.beam(
                    v,
                    Math.cos(a) * radius,
                    .04,
                    Math.sin(a) * radius,
                    Math.cos(b) * radius,
                    .04,
                    Math.sin(b) * radius,
                    .014,
                    color);
        }
    }

    public static final class AsterismAltarRenderer
            extends OmphalosBlockEntityRenderer<AsterismAltarBlockEntity> {
        public AsterismAltarRenderer() {
            super(64);
        }

        protected void renderModel(AsterismAltarBlockEntity be, WorldDraw d, int l, int o) {
            if (be.tier().ordinal() == 0 || !be.isFormed()) return;
            var mesh =
                    be.tier().ordinal() == 1
                            ? MachineMeshes.ALTAR_TIER_TWO
                            : MachineMeshes.ALTAR_TIER_THREE;
            int parts = be.tier().ordinal() == 1 ? 4 : 8;
            for (int i = 0; i < parts; i++) {
                double a = time(be, d) * .01 + i * Math.PI * 2 / parts;
                d.pose.pushPose();
                try {
                    d.pose.translate(
                            Math.cos(a) * .8, 1.2 + Math.sin(a * 2) * .1, Math.sin(a) * .8);
                    d.pose.mulPose(d.rotate(0, (float) -a, 0));
                    mesh.emit(d, d.solid(), 0xffacbfdb);
                } finally {
                    d.pose.popPose();
                }
            }
            d.pose.pushPose();
            try {
                d.pose.translate(0, 1.25, 0);
                crystal(d, 0xffb6e7ff);
            } finally {
                d.pose.popPose();
            }
            if (be.visualWorking()) {
                halo(d, 1.7, 0x669fdcff);
                d.beam(d.buffer(Kind.BEAM_ADDITIVE), 0, 1, 0, 0, 4, 0, .07, 0x4497ceff);
            }
        }
    }

    public static final class ResonanceAltarRenderer
            extends OmphalosBlockEntityRenderer<MachineBlockEntity> {
        public ResonanceAltarRenderer() {
            super(48);
        }

        protected void renderModel(MachineBlockEntity be, WorldDraw d, int l, int o) {
            MachineMeshes.ALTAR_RESONANCE.emit(d, d.solid(), 0xff8ea1b3);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4 + (be.charge() > 0 ? 0 : time(be, d) * .007);
                d.box(
                        d.buffer(Kind.GLOW_LAYER),
                        Math.cos(a),
                        1.2 + Math.min(.5, be.charge() / 100),
                        Math.sin(a),
                        .12,
                        .12,
                        .12,
                        0xffb2b8ef);
            }
        }
    }

    public static final class ResonanceRelayRenderer
            extends OmphalosBlockEntityRenderer<CraftingRelayBlockEntity> {
        public ResonanceRelayRenderer() {
            super(48);
        }

        protected void renderModel(CraftingRelayBlockEntity b, WorldDraw d, int l, int o) {
            item(b.visualItem(), b, d, l, o, 1.1);
        }
    }

    public static final class InfuserRenderer
            extends OmphalosBlockEntityRenderer<LumenInfuserBlockEntity> {
        public InfuserRenderer() {
            super(48);
        }

        protected void renderModel(LumenInfuserBlockEntity b, WorldDraw d, int l, int o) {
            item(b.visualItem(), b, d, l, o, 1.4);
            if (b.visualWorking()) halo(d, .8, 0x669bcfff);
        }
    }

    public static final class ChaliceRenderer
            extends OmphalosBlockEntityRenderer<LumenChaliceBlockEntity> {
        public ChaliceRenderer() {
            super(32);
        }

        protected void renderModel(LumenChaliceBlockEntity b, WorldDraw d, int l, int o) {
            var f = b.tank().getFluidInTank(0);
            if (f.isEmpty()) return;
            d.pose.translate(0, 1.4, 0);
            d.pose.mulPose(d.rotate((float) (time(b, d) * .013), (float) (time(b, d) * .02), 0));
            com.mpp.stellaeomphalos.client.render.util.FluidRenderHelper.render(
                    d, f, b.tank().getTankCapacity(0), true);
        }
    }

    public static final class LumenWellRenderer
            extends OmphalosBlockEntityRenderer<LumenWellBlockEntity> {
        public LumenWellRenderer() {
            super(32);
        }

        protected void renderModel(LumenWellBlockEntity b, WorldDraw d, int l, int o) {
            item(b.visualItem(), b, d, l, o, 1.2);
            var f = b.tank().getFluidInTank(0);
            com.mpp.stellaeomphalos.client.render.util.FluidRenderHelper.render(
                    d, f, b.tank().getTankCapacity(0), false);
        }
    }

    public static final class CollectorRenderer
            extends OmphalosBlockEntityRenderer<LumenContent.Collector> {
        public CollectorRenderer() {
            super(64);
        }

        protected void renderModel(LumenContent.Collector b, WorldDraw d, int l, int o) {
            d.pose.translate(0, 1.1 + Math.cos(time(b, d) * Math.PI / 128) * .03, 0);
            crystal(d, 0xffb6e7ff);
            if (b.data().seesSky())
                d.beam(d.buffer(Kind.BEAM_ADDITIVE), 0, .8, 0, 0, 6, 0, .055, 0x449acaff);
        }
    }

    public static final class GrindstoneRenderer
            extends OmphalosBlockEntityRenderer<GrindwheelBlockEntity> {
        public GrindstoneRenderer() {
            super(48);
        }

        protected void renderModel(GrindwheelBlockEntity b, WorldDraw d, int l, int o) {
            d.pose.pushPose();
            try {
                d.pose.translate(0, 1.1, 0);
                d.pose.mulPose(d.rotate(b.visualWorking() ? (float) (time(b, d) * .314) : 0, 0, 0));
                MachineMeshes.GRINDSTONE.emit(d, d.solid(), 0xff9592b2);
            } finally {
                d.pose.popPose();
            }
            item(b.visualItem(), b, d, l, o, 1.55);
        }
    }

    public static final class LensRenderer extends OmphalosBlockEntityRenderer<MachineBlockEntity> {
        public LensRenderer() {
            super(64);
        }

        protected void renderModel(MachineBlockEntity b, WorldDraw d, int l, int o) {
            d.pose.mulPose(
                    d.rotate(
                            0,
                            (float)
                                    Math.toRadians(
                                            b.getBlockState()
                                                    .getValue(
                                                            com.mpp.stellaeomphalos.content.block
                                                                    .PartSixBlocks.FACING)
                                                    .toYRot()),
                            0));
            MachineMeshes.LENS.emit(d, d.solid(), 0xff8a8ba1);
            MachineMeshes.LENS_COLOR.emit(d, d.buffer(Kind.SOFT_PARTICLE), lensTint(b, 0x88));
        }
    }

    public static final class StarChartTableRenderer
            extends OmphalosBlockEntityRenderer<MachineBlockEntity> {
        public StarChartTableRenderer() {
            super(48);
        }

        protected void renderModel(MachineBlockEntity b, WorldDraw d, int l, int o) {
            MachineMeshes.STAR_CHART_TABLE.emit(d, d.solid(), 0xff8d7794);
            if (b.parchment() > 0)
                d.quad(d.buffer(Kind.GLOW_LAYER), 0, .88, 0, .3, 0, 0, 0, 0, .3, 0xffefdfbe);
            else item(b.input(), b, d, l, o, 1);
            if (!b.glass().isEmpty())
                d.box(d.buffer(Kind.GHOST_BLOCK), 0, 1, 0, .42, .12, .42, 0x449cdfff);
            if (b.runTick() > 0) halo(d, .7, 0x669bcfff);
        }
    }

    public static final class PrismRenderer
            extends OmphalosBlockEntityRenderer<MachineBlockEntity> {
        public PrismRenderer() {
            super(64);
        }

        protected void renderModel(MachineBlockEntity b, WorldDraw d, int l, int o) {
            crystal(d, 0xffb6e7ff);
            MachineMeshes.PRISM_COLOR.emit(d, d.buffer(Kind.GHOST_BLOCK), lensTint(b, 0x99));
        }
    }

    public static final class RitePedestalRenderer
            extends OmphalosBlockEntityRenderer<RitePedestalBlockEntity> {
        public RitePedestalRenderer() {
            super(48);
        }

        protected void renderModel(RitePedestalBlockEntity b, WorldDraw d, int l, int o) {
            item(b.crystal(), b, d, l, o, 1.3);
            if (b.displayedProgress() > 0) halo(d, 1.1, 0x668cdfff);
        }
    }

    public static final class ObservatoryRenderer
            extends OmphalosBlockEntityRenderer<TechnicalBlockEntity> {
        public ObservatoryRenderer() {
            super(64);
        }

        protected void renderModel(TechnicalBlockEntity b, WorldDraw d, int l, int o) {
            d.pose.translate(0, 1.3, 0);
            var p = Minecraft.getInstance().player;
            float yaw = p != null && p.isPassenger() ? p.getViewYRot(d.partial) : 0,
                    pitch = p != null && p.isPassenger() ? p.getViewXRot(d.partial) : 25;
            d.pose.mulPose(
                    d.rotate((float) Math.toRadians(pitch), (float) Math.toRadians(-yaw), 0));
            MachineMeshes.OBSERVATORY.emit(d, d.solid(), 0xffaaa0b8);
        }
    }

    public static final class PhantomBlockEntityRenderer
            extends OmphalosBlockEntityRenderer<TechnicalBlockEntity> {
        public PhantomBlockEntityRenderer() {
            super(48);
        }

        protected void renderModel(TechnicalBlockEntity b, WorldDraw d, int l, int o) {
            com.mpp.stellaeomphalos.client.render.PhantomBatch.draw(b.hostState(), d, 0x668bcaff);
        }
    }

    public static final class PhantomTreeRenderer
            extends OmphalosBlockEntityRenderer<TechnicalBlockEntity> {
        public PhantomTreeRenderer() {
            super(48);
        }

        protected void renderModel(TechnicalBlockEntity b, WorldDraw d, int l, int o) {
            com.mpp.stellaeomphalos.client.render.PhantomBatch.draw(b.hostState(), d, 0x667fe2bb);
        }
    }

    public static final class GatewayRenderer
            extends OmphalosBlockEntityRenderer<TechnicalBlockEntity> {
        public GatewayRenderer() {
            super(64);
        }

        @Override
        public boolean shouldRenderOffScreen(TechnicalBlockEntity b) {
            return true;
        }

        protected void renderModel(TechnicalBlockEntity b, WorldDraw d, int l, int o) {
            if ((!com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.flag("render.gatewayShield")
                    || !com.mpp.stellaeomphalos.OmphalosConfig.CLIENT.flag("effects.composite")))
                return;
            var v = d.buffer(Kind.GHOST_BLOCK);
            for (int row = 0; row < 12; row++) {
                double a = row * Math.PI / 12, beta = (row + 1) * Math.PI / 12;
                for (int col = 0; col < 24; col++) {
                    double c = col * Math.PI / 12, e = (col + 1) * Math.PI / 12;
                    d.vertex(
                            v,
                            Math.sin(a) * Math.cos(c) * 1.4,
                            1 + Math.cos(a) * 1.4,
                            Math.sin(a) * Math.sin(c) * 1.4,
                            0x226faaff,
                            0,
                            0);
                    d.vertex(
                            v,
                            Math.sin(beta) * Math.cos(c) * 1.4,
                            1 + Math.cos(beta) * 1.4,
                            Math.sin(beta) * Math.sin(c) * 1.4,
                            0x226faaff,
                            1,
                            0);
                    d.vertex(
                            v,
                            Math.sin(beta) * Math.cos(e) * 1.4,
                            1 + Math.cos(beta) * 1.4,
                            Math.sin(beta) * Math.sin(e) * 1.4,
                            0x226faaff,
                            1,
                            1);
                    d.vertex(
                            v,
                            Math.sin(a) * Math.cos(e) * 1.4,
                            1 + Math.cos(a) * 1.4,
                            Math.sin(a) * Math.sin(e) * 1.4,
                            0x226faaff,
                            0,
                            1);
                }
            }
        }
    }
}
