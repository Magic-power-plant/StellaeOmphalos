package com.mpp.stellaeomphalos.constellation.boon;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoonFreePointLedgerTest {

    @Test
    void fixedTokensGrantIdempotently() {
        var ledger = new BoonFreePointLedger();
        assertTrue(ledger.grant("stellaeomphalos:core/root#1"));
        assertFalse(ledger.grant("stellaeomphalos:core/root#1"), "Fixed token must grant only once");
        ledger.grantAll(CoreRootBoonNode.BONUS_TOKENS);
        ledger.grantAll(CoreRootBoonNode.BONUS_TOKENS);
        assertEquals(3, ledger.count(), "Core root bonus is exactly three points, granted once");
    }

    @Test
    void revokeReclaimsExactlyTheRecordedTokens() {
        var ledger = new BoonFreePointLedger();
        ledger.grant("connector:a->b");
        ledger.grant("connector:a->c");
        ledger.grant("unrelated");
        assertTrue(ledger.revoke("connector:a->b"));
        assertFalse(ledger.revoke("connector:a->b"));
        assertEquals(2, ledger.count());
        ledger.revokeAll(List.of("connector:a->c"));
        assertEquals(List.of("unrelated"), ledger.tokens(), "Only the recorded tokens are reclaimed");
        ledger.clear();
        assertEquals(0, ledger.count());
    }

    @Test
    void emptyTokenRejected() {
        var ledger = new BoonFreePointLedger();
        assertThrows(IllegalArgumentException.class, () -> ledger.grant(""));
    }
}
