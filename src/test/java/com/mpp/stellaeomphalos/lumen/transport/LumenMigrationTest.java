package com.mpp.stellaeomphalos.lumen.transport;

import com.mpp.stellaeomphalos.data.loader.MigrationReports;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LumenMigrationTest {
    @Test void legacyLowercasePayloadMigratesTransactionallyToCanonicalKeys() {
        MigrationReports.drain();
        var tag = new CompoundTag(); tag.putInt("dataVersion", 1); tag.putString("domain", LumenNetworkData.KEY);
        var payload = new CompoundTag(); var sources = new ListTag(); var source = new CompoundTag();
        source.putLong("pos", 12); source.putString("provider", "test:source"); source.putLong("output", 55);
        source.putBoolean("autoLink", true); source.putBoolean("seesSky", true); source.putBoolean("enhanced", false);
        sources.add(source); payload.put("sources", sources); payload.put("nodes", new ListTag()); tag.put("payload", payload);
        var original = tag.copy(); var data = LumenNetworkData.load(tag);
        assertFalse(data.readOnly()); assertEquals(55, data.state().sources().get(0).output());
        assertEquals(original, tag);
        var saved = data.save(new CompoundTag()); assertEquals(2, saved.getInt("dataVersion"));
        var fields = saved.getCompound("payload").getList("Sources", Tag.TAG_COMPOUND).getCompound(0);
        assertTrue(fields.contains("Pos") && fields.contains("AutoLink") && !fields.contains("pos"));
        assertEquals(data.state(), LumenNetworkData.load(saved).state());
        assertTrue(MigrationReports.drain().containsKey(LumenNetworkData.KEY));
    }
}
