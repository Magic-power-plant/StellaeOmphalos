package com.mpp.stellaeomphalos.data.loader;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionArchiveTest {
    private static final ArchiveKey KEY = new ArchiveKey("test_archive", 2);

    @Test
    void missingSchemaUsesVersionOneDefaults() {
        var archive = new TestArchive();
        var tag = new CompoundTag();
        tag.putInt("value", 7);
        archive.restore(tag);
        assertFalse(archive.readOnly());
        assertEquals(7, archive.value);
        assertEquals(2, archive.save(new CompoundTag()).getInt("schema"));
    }

    @Test
    void futureSchemaIsReadOnlyAndDoesNotWriteUnknownFields() {
        var archive = new TestArchive();
        var tag = new CompoundTag();
        tag.putInt("schema", 99);
        tag.putInt("value", 42);
        archive.restore(tag);
        assertTrue(archive.readOnly());
        var saved = archive.save(new CompoundTag());
        assertEquals(2, saved.getInt("schema"));
        assertFalse(saved.contains("value"));
    }

    @Test
    void invalidPayloadBecomesReadOnlyInsteadOfThrowingDuringRestore() {
        var archive = new TestArchive();
        var tag = new CompoundTag();
        tag.putInt("schema", 2);
        tag.putBoolean("valid", false);
        archive.restore(tag);
        assertTrue(archive.readOnly());
    }

    private static final class TestArchive extends DimensionArchive {
        private int value;

        private TestArchive() { super(KEY); }

        @Override
        protected void readData(CompoundTag tag) {
            if (!tag.getBoolean("valid") && tag.contains("valid")) throw new IllegalArgumentException("invalid");
            value = tag.getInt("value");
        }

        @Override
        protected void writeData(CompoundTag tag) { tag.putInt("value", value); }
    }
}
