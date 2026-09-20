package com.mpp.stellaeomphalos.lumen.transport.stasis;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StasisFilterTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();

    @Test void all_except_truth_table() {
        var mode = StasisFilter.Mode.ALL_EXCEPT;
        assertFalse(StasisFilter.decide(mode, OWNER, true, true, OWNER), "owner player never frozen");
        assertTrue(StasisFilter.decide(mode, OWNER, true, true, STRANGER), "other players frozen when targeted");
        assertFalse(StasisFilter.decide(mode, OWNER, false, true, STRANGER), "players spared when targetPlayers=false");
        assertTrue(StasisFilter.decide(mode, OWNER, true, false, STRANGER), "non-player entities frozen");
        assertTrue(StasisFilter.decide(mode, OWNER, false, false, STRANGER), "non-player entities frozen regardless");
        assertTrue(StasisFilter.decide(mode, null, true, true, STRANGER), "ownerless zone freezes players");
    }

    @Test void no_players_truth_table() {
        var mode = StasisFilter.Mode.NO_PLAYERS;
        assertFalse(StasisFilter.decide(mode, OWNER, true, true, OWNER));
        assertFalse(StasisFilter.decide(mode, null, true, true, STRANGER));
        assertTrue(StasisFilter.decide(mode, null, true, false, STRANGER), "mobs frozen");
        assertTrue(StasisFilter.decide(mode, null, false, false, STRANGER));
    }
}
