package com.mpp.stellaeomphalos.network;

import com.mpp.stellaeomphalos.network.toClient.PktConfigVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetworkFoundationTest {
    @Test void reassemblesOutOfOrderWithDuplicatesAndReleasesSession() {
        byte[] expected = new byte[30001]; new Random(42).nextBytes(expected);
        var parts = new ArrayList<>(ChunkedEnvelope.split(7, expected));
        Collections.reverse(parts);
        var assembler = new ChunkAssembler(); var player = UUID.randomUUID();
        assertTrue(assembler.accept(player, parts.get(0), 0).isEmpty());
        assertTrue(assembler.accept(player, parts.get(0), 1).isEmpty());
        assertTrue(assembler.accept(player, parts.get(1), 2).isEmpty());
        var complete = assembler.accept(player, parts.get(2), 3).orElseThrow();
        assertEquals(7, complete.payloadId()); assertArrayEquals(expected, complete.bytes());
        assertEquals(0, assembler.sessionCount());
    }
    @Test void networkBudgetHasGlobalAndIsolationStages() {
        var budget = new NetworkBudget(); var player = UUID.randomUUID();
        var policy = new NetworkBudget.Policy(1, 1, 1, 1, false);
        assertTrue(budget.acquire(player, 1, policy, 0).accepted());
        assertEquals(NetworkBudget.Action.DROP, budget.acquire(player, 1, policy, 0).action());
        for (int i = 0; i < 8; i++) budget.acquire(player, 1, policy, 0);
        assertEquals(NetworkBudget.Action.ISOLATE, budget.acquire(player, 1, policy, 0).action());
        assertEquals(NetworkBudget.Action.DISCONNECT, budget.acquire(player, 1, policy, 0).action());
    }
    @Test void timeoutIsFixedAndCannotBeExtendedByDuplicates() {
        var part = ChunkedEnvelope.split(1, new byte[20000]).get(0);
        var assembler = new ChunkAssembler(); var player = UUID.randomUUID();
        assembler.accept(player, part, 0); assembler.accept(player, part, 2999999999L);
        assembler.expire(3000000000L); assertEquals(0, assembler.sessionCount());
    }
    @Test void rejectsInconsistentHeadersAndOversizedBuffers() {
        assertThrows(IllegalArgumentException.class, () -> ChunkedEnvelope.split(1, new byte[ChunkedEnvelope.MAX_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> new ChunkedEnvelope(UUID.randomUUID(), 512, 0, 1, 12, new byte[8192]));
        var parts = ChunkedEnvelope.split(1, new byte[20000]);
        var assembler = new ChunkAssembler(); var player = UUID.randomUUID();
        assembler.accept(player, parts.get(0), 0);
        assertThrows(IllegalArgumentException.class, () -> assembler.accept(player,
                new ChunkedEnvelope(parts.get(0).session(), 2, 1, 2, 20000,
                        new byte[20000 - ChunkedEnvelope.CHUNK_SIZE]), 1));
        assertEquals(0, assembler.sessionCount());
    }
    @Test void rateLimitRefillsAndWarnsOncePerMinute() {
        var throttle = new NetworkThrottle(); var player = UUID.randomUUID();
        for (int i = 0; i < 3; i++) assertTrue(throttle.acquire(player, 1, 0, 3, 0.01).accepted());
        assertTrue(throttle.acquire(player, 1, 0, 3, 0.01).warn());
        assertFalse(throttle.acquire(player, 1, 1, 3, 0.01).warn());
        assertTrue(throttle.acquire(player, 1, 100, 3, 0.01).accepted());
        throttle.remove(player); assertEquals(0, throttle.bucketCount());
    }
    @Test void gateRequiresReadyAndResetsOnTimeoutAndDisconnect() {
        var gate = new ClientSyncGate();
        assertFalse(gate.allows(0, 0)); gate.begin(new ProtocolVersion(1, 2), 0);
        assertFalse(gate.allows(0, 1)); assertTrue(gate.ready(2)); assertFalse(gate.ready(3));
        assertTrue(gate.allows(2, 4)); assertFalse(gate.allows(3, 4));
        gate.reset(); assertFalse(gate.allows(0, 4));
        gate.begin(new ProtocolVersion(1, 0), 0); assertFalse(gate.ready(3000000000L));
        assertEquals(ClientSyncGate.State.CLOSED, gate.state());
    }
    @Test void codecRegistryRejectsDuplicatesWrongDirectionAndMalformedPayloads() {
        var registry = new PayloadRegistry();
        registry.register(0, PayloadRegistry.Direction.TO_CLIENT, PktConfigVersion.class, PktConfigVersion.CODEC, 0);
        assertThrows(IllegalStateException.class, () -> registry.register(0, PayloadRegistry.Direction.TO_CLIENT, PktConfigVersion.class, PktConfigVersion.CODEC, 0));
        byte[] bytes = registry.encode(new PktConfigVersion(3));
        assertEquals(new PktConfigVersion(3), registry.decode(0, PayloadRegistry.Direction.TO_CLIENT, bytes));
        assertThrows(IllegalArgumentException.class, () -> registry.decode(0, PayloadRegistry.Direction.TO_SERVER, bytes));
        assertThrows(RuntimeException.class, () -> registry.decode(0, PayloadRegistry.Direction.TO_CLIENT, new byte[]{123}));
    }
    @Test void protocolsRejectUnknownAndForeignMajor() {
        assertTrue(ProtocolVersion.CURRENT.accepts("1.7"));
        assertFalse(ProtocolVersion.CURRENT.accepts("2.0")); assertFalse(ProtocolVersion.CURRENT.accepts("ABSENT"));
    }
}
