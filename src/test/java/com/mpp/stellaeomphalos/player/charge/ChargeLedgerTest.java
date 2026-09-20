package com.mpp.stellaeomphalos.player.charge;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChargeLedgerTest {
    private static final float BASE = 0.01F;

    @Test void regen_curve_matches_contract() {
        assertEquals(0.06F, ChargeLedger.regenGain(BASE, 1.0F, true), 1.0E-6, "day + open sky");
        assertEquals(0.03F, ChargeLedger.regenGain(BASE, 0.0F, true), 1.0E-6, "night + open sky");
        assertEquals(0.01F, ChargeLedger.regenGain(BASE, 1.0F, false), 1.0E-6, "day indoors");
        assertEquals(0.005F, ChargeLedger.regenGain(BASE, 0.0F, false), 1.0E-6, "night indoors");
    }

    @Test void regen_caps_at_full_and_creative_skips() {
        assertEquals(1.0F, ChargeLedger.tickValue(0.999F, BASE, 1.0F, true, false), "cap at 1.0");
        assertEquals(1.0F, ChargeLedger.tickValue(0.25F, BASE, 0.0F, false, true), "creative pinned to full");
        assertEquals(0.25F + 0.06F, ChargeLedger.tickValue(0.25F, BASE, 1.0F, true, false), 1.0E-6);
    }

    @Test void first_access_initializes_full_and_disconnect_cleans() {
        var ledger = new ChargeLedger();
        var id = UUID.randomUUID();
        assertEquals(1.0F, ledger.charge(id));
        ledger.drain(id, 1.0F, false);
        assertEquals(0.0F, ledger.charge(id));
        ledger.remove(id);
        assertEquals(1.0F, ledger.charge(id), "re-initialized after removal");
    }

    @Test void drain_simulate_does_not_consume() {
        var ledger = new ChargeLedger();
        var id = UUID.randomUUID();
        assertTrue(ledger.drain(id, 0.4F, true));
        assertEquals(1.0F, ledger.charge(id), "simulate must not deduct");
        assertTrue(ledger.drain(id, 0.4F, false));
        assertEquals(0.6F, ledger.charge(id), 1.0E-6);
        assertFalse(ledger.drain(id, 0.7F, true), "overdraft probe must fail");
        assertEquals(0.6F, ledger.charge(id), 1.0E-6, "failed probe changed state");
    }

    @Test void sync_only_below_full_and_on_quantized_change() {
        var ledger = new ChargeLedger();
        var id = UUID.randomUUID();
        ledger.charge(id);
        assertFalse(ledger.shouldSync(id, 1.0F), "never sync at full");
        ledger.markSynced(id, 1.0F);
        assertTrue(ledger.shouldSync(id, 0.5F), "drop below full syncs");
        ledger.markSynced(id, 0.5F);
        assertFalse(ledger.shouldSync(id, 0.5F + 0.4F / ChargeLedger.QUANTUM), "sub-quantum change must not sync");
        assertTrue(ledger.shouldSync(id, 0.5F + 1.0F / ChargeLedger.QUANTUM), "quantum step syncs");
        assertEquals(0, ChargeLedger.quantize(-0.5F));
        assertEquals(ChargeLedger.QUANTUM, ChargeLedger.quantize(2.0F));
    }
}
