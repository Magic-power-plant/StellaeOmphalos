package com.mpp.stellaeomphalos.lumen.transport.stasis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StasisDataTest {
    private static StasisData.State sample() {
        return new StasisData.State(List.of(
                new StasisData.ZoneRecord(new BlockPos(10, 64, -20).asLong(), 8.5F, "all_except",
                        Optional.of(UUID.fromString("11111111-2222-3333-4444-555555555555")), true, 1234, 1),
                new StasisData.ZoneRecord(new BlockPos(-5, 70, 6).asLong(), 2.0F, "no_players",
                        Optional.empty(), false, 77, 0)));
    }

    @Test void save_restore_roundtrip_preserves_state() {
        var data = new StasisData();
        data.store(sample());
        var tag = data.save(new CompoundTag());
        assertEquals(1, tag.getInt("dataVersion"));
        assertEquals("stellaeomphalos_stasis", tag.getString("domain"));
        var restored = new StasisData();
        restored.restore(tag, 99L);
        assertEquals(data.state(), restored.state(), "zone records must survive the roundtrip");
    }

    @Test void payload_keys_are_pascal_case() {
        var data = new StasisData();
        data.store(sample());
        var tag = data.save(new CompoundTag());
        var zones = tag.getCompound("payload").getList("Zones", Tag.TAG_COMPOUND);
        assertEquals(2, zones.size());
        var first = zones.getCompound(0);
        for (String key : List.of("Center", "Radius", "FilterMode", "Owner", "TargetPlayers", "Remaining", "ParticleTier"))
            assertTrue(first.contains(key), "missing PascalCase key " + key);
        assertTrue(first.getString("FilterMode").equals("all_except"));
        assertTrue(first.contains("Owner") && !zones.getCompound(1).contains("Owner"), "optional owner handling broken");
    }
}
