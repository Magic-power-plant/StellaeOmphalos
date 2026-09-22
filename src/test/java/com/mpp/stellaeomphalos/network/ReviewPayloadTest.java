package com.mpp.stellaeomphalos.network;

import com.mojang.serialization.JsonOps;
import com.mpp.stellaeomphalos.network.toServer.PktImprintEngrave;
import com.mpp.stellaeomphalos.network.toClient.PreviewStartPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReviewPayloadTest {
    @Test void engravingAuthorityFieldsRoundTripAndStrokeLimitRejectsBothDirections() {
        var packet = new PktImprintEngrave(17, new ResourceLocation("minecraft", "the_nether"),
                new BlockPos(3, 70, -5), 2, List.of(new PktImprintEngrave.Stroke(3, 0, 1)));
        var json = PktImprintEngrave.CODEC.encodeStart(JsonOps.INSTANCE, packet).result().orElseThrow();
        assertEquals(packet, PktImprintEngrave.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow());
        var oversized = new PktImprintEngrave(17, packet.dimension(), packet.origin(), 2,
                Collections.nCopies(65, new PktImprintEngrave.Stroke(3, 0, 1)));
        assertTrue(PktImprintEngrave.CODEC.encodeStart(JsonOps.INSTANCE, oversized).error().isPresent());
        var array = json.getAsJsonObject().getAsJsonArray("strokes");
        for (int i = 0; i < 64; i++) array.add(array.get(0).deepCopy());
        assertTrue(PktImprintEngrave.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());
    }
    @Test void previewLimitFollowsRegisteredTypeInsteadOfMagicId() {
        var registry = new PayloadRegistry();
        registry.register(99, PayloadRegistry.Direction.TO_CLIENT, PreviewStartPayload.class, PreviewStartPayload.CODEC, 0);
        var packet = new PreviewStartPayload(new ResourceLocation("test", "a".repeat(9000)), BlockPos.ZERO, 0, 2, 10, 1);
        assertThrows(IllegalArgumentException.class, () -> registry.encode(packet));
        var json = PreviewStartPayload.CODEC.encodeStart(JsonOps.INSTANCE, packet).result().orElseThrow().toString();
        assertThrows(IllegalArgumentException.class, () -> registry.decode(99, PayloadRegistry.Direction.TO_CLIENT,
                json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
