package com.mpp.stellaeomphalos.client;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.client.effect.*;
import com.mpp.stellaeomphalos.client.render.res.SpriteStrip;
import com.mpp.stellaeomphalos.client.render.util.WorldDraw;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

/** Small pure contracts for the client scheduler; no Minecraft client instance is needed. */
class ClientContractsTest {
    private static final class Probe extends AbstractEffectTrack {
        Probe(boolean mandatory, int priority) {
            super(EffectLane.WORLD, 20, priority, mandatory);
        }

        public double distanceSq(double x, double y, double z) {
            return 0;
        }

        public void collect(WorldDraw draw) {}
    }

    @Test
    void budgetDropsLowestPriorityButKeepsMandatoryTrack() {
        var director = new EffectDirector();
        director.budget(1);
        director.spawn(new Probe(false, 1));
        var required = director.spawn(new Probe(true, 0));
        director.tickAll();
        assertTrue(required.isMandatory());
        assertFalse(required.isExpired());
        assertEquals(1, director.trackCount());
        assertTrue(director.dropped() >= 1);
    }

    @Test
    void stripUsesWallClockFrameIndexAndFullValueEquality() {
        var id = new ResourceLocation("stellaeomphalos", "textures/effect/test.png");
        var a = new SpriteStrip(id, 2, 4, 50);
        var b = new SpriteStrip(id, 2, 4, 50);
        assertEquals(a, b);
        assertEquals(0, a.frame(0));
        assertEquals(7, a.frame(350));
        assertEquals(0, a.frame(400));
        assertEquals(0.25F, a.u0(50), 0.0001F);
        assertEquals(0.5F, a.v0(200), 0.0001F);
    }

    @Test
    void spawnDuringTickDoesNotInvalidateActiveIteration() {
        var director = new EffectDirector();
        director.budget(2);
        var emitter =
                new AbstractEffectTrack(EffectLane.WORLD, 20, 100, true) {
                    protected void advance() {
                        director.spawn(new Probe(false, 20));
                    }

                    public double distanceSq(double x, double y, double z) {
                        return 0;
                    }

                    public void collect(WorldDraw draw) {}
                };
        director.spawn(emitter);
        director.spawn(new Probe(false, 0));
        assertDoesNotThrow(director::tickAll);
        assertEquals(2, director.trackCount());
        director.clearAll();
        assertEquals(0, director.trackCount());
        assertTrue(emitter.isExpired());
    }

    @Test
    void drawnUndirectedEdgesMatchOnlyTheCompletePattern() {
        var a = new com.mpp.stellaeomphalos.constellation.starmap.StarPoint(2, 2);
        var b = new com.mpp.stellaeomphalos.constellation.starmap.StarPoint(8, 8);
        var c = new com.mpp.stellaeomphalos.constellation.starmap.StarPoint(12, 2);
        var canvas =
                new com.mpp.stellaeomphalos.client.screen.SignCanvas(java.util.List.of(a, b, c));
        var template =
                java.util.List.of(
                        new com.mpp.stellaeomphalos.constellation.starmap.StarLine(a, b),
                        new com.mpp.stellaeomphalos.constellation.starmap.StarLine(b, c));
        canvas.begin(8, 8, 1);
        assertTrue(canvas.release(2, 2, 1));
        assertFalse(canvas.matches(template));
        canvas.begin(12, 2, 1);
        assertTrue(canvas.release(8, 8, 1));
        assertTrue(canvas.matches(template));
        canvas.undo();
        assertFalse(canvas.matches(template));
    }

    @Test
    void observationProofRejectsDuplicatesAndAcceptsReverseDirection() {
        var a = new com.mpp.stellaeomphalos.constellation.starmap.StarPoint(1, 1);
        var b = new com.mpp.stellaeomphalos.constellation.starmap.StarPoint(4, 4);
        var edge = new com.mpp.stellaeomphalos.constellation.starmap.StarLine(a, b);
        var proof = new com.mpp.stellaeomphalos.network.toServer.PktObserveSign.Edge(4, 4, 1, 1);
        assertTrue(
                com.mpp.stellaeomphalos.content.item.knowledge.ObservationProtocol.matches(
                        java.util.List.of(proof), java.util.List.of(edge)));
        assertFalse(
                com.mpp.stellaeomphalos.content.item.knowledge.ObservationProtocol.matches(
                        java.util.List.of(proof, proof), java.util.List.of(edge)));
    }

    @Test
    void cameraSequenceEndsWithoutChangingItsPathAndInterpolatesAcrossYawWrap() {
        var sequence =
                new com.mpp.stellaeomphalos.client.view.ViewSequence(
                        java.util.List.of(
                                new com.mpp.stellaeomphalos.client.view.ViewSequence.Keyframe(
                                        0, 1, 0, 0, 179, 0, 70),
                                new com.mpp.stellaeomphalos.client.view.ViewSequence.Keyframe(
                                        10, 1, 0, 10, -179, 0, 70)),
                        1);
        for (int i = 0; i < 5; i++) sequence.tick();
        var position = new org.joml.Vector3d();
        sequence.sample(0, position);
        assertEquals(5, position.x, 1e-6);
        assertEquals(180, sequence.yaw(), 1e-4);
        for (int i = 0; i < 5; i++) sequence.tick();
        assertTrue(sequence.finished());
    }

    @Test
    void paletteIgnoresTransparentPixelsAndIsDeterministic() {
        int[] colors = {0xffff0000, 0xffff0000, 0xff0000ff, 0x0000ff00, 0xffffffff};
        int color = com.mpp.stellaeomphalos.client.image.PaletteExtractor.dominantColor(colors);
        assertTrue((color >> 16 & 255) > 200);
        assertTrue((color & 255) < 30);
        assertEquals(
                color, com.mpp.stellaeomphalos.client.image.PaletteExtractor.dominantColor(colors));
    }

    @Test
    void hudFadeExtendsHoldAndReturnsToZero() {
        var fade = new com.mpp.stellaeomphalos.client.hud.HudFadeState();
        fade.request(40);
        for (int i = 0; i < 20; i++) fade.tick();
        assertEquals(1, fade.alpha(), 1e-5);
        for (int i = 0; i < 30; i++) fade.tick();
        assertEquals(0, fade.alpha());
    }

    @Test void mandatoryRenderingSurvivesDisabledDecorations() {
        int[] emitted={0};
        var director=new com.mpp.stellaeomphalos.client.effect.EffectDirector();
        director.spawn(new com.mpp.stellaeomphalos.client.effect.AbstractEffectTrack(com.mpp.stellaeomphalos.client.effect.EffectLane.GHOST,20,100,true){
            public double distanceSq(double x,double y,double z){return 0;}
            public void collect(com.mpp.stellaeomphalos.client.render.util.WorldDraw draw){emitted[0]++;}
        });
        director.budget(0);director.tickAll();
        director.flush(com.mpp.stellaeomphalos.client.effect.EffectLane.GHOST,new com.mpp.stellaeomphalos.client.render.util.WorldDraw(),100,false);
        assertEquals(1,emitted[0]);
    }

    @Test void clientStarGeometryIsReplacedAndClearedAsOneSnapshot() {
        var id=new net.minecraft.resources.ResourceLocation("stellaeomphalos:remote_only");
        var data=new net.minecraft.nbt.CompoundTag();var definition=new net.minecraft.nbt.CompoundTag();
        definition.putInt("Number",500);definition.putInt("Color",0xff00ffff);definition.putBoolean("Major",true);
        var stars=new net.minecraft.nbt.ListTag();
        for(int coordinate:new int[]{2,8}){var point=new net.minecraft.nbt.CompoundTag();point.putInt("X",coordinate);point.putInt("Y",coordinate);stars.add(point);}
        definition.put("Stars",stars);var edges=new net.minecraft.nbt.ListTag();var edge=new net.minecraft.nbt.CompoundTag();
        edge.putInt("AX",2);edge.putInt("AY",2);edge.putInt("BX",8);edge.putInt("BY",8);edges.add(edge);definition.put("Edges",edges);data.put(id.toString(),definition);
        com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.replace(data);
        var sign=com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byNumber(500);
        assertEquals(id,sign.id());assertEquals(2,sign.stars().size());assertEquals(1,sign.lines().size());
        assertTrue(com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.major(sign));
        com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.clear();assertNull(com.mpp.stellaeomphalos.client.sign.SignDefinitionMirror.byNumber(500));
    }
}
