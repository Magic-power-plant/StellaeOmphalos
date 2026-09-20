package com.mpp.stellaeomphalos.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import com.mpp.stellaeomphalos.player.progress.*;

import net.minecraft.nbt.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;
import java.util.*;

class StarRecordIOTest {
    static final class MemoryFiles implements StarRecordIO.Filesystem {
        final Map<Path, CompoundTag> files = new HashMap<>();
        final Set<Path> broken = new HashSet<>();
        boolean failWrites, failCopies;

        public CompoundTag read(Path p) throws IOException {
            if (broken.contains(p) || !files.containsKey(p))
                throw new IOException("injected read failure");
            return files.get(p).copy();
        }

        public void write(Path p, CompoundTag t) throws IOException {
            if (failWrites) throw new IOException("injected disk full");
            files.put(p, t.copy());
        }

        public void copy(Path a, Path b) throws IOException {
            if (failCopies) throw new IOException("injected read-only repair");
            files.put(b, files.getOrDefault(a, new CompoundTag()).copy());
            broken.remove(b);
        }

        public void replace(Path a, Path b) throws IOException {
            if (failWrites) throw new IOException("injected move failure");
            files.put(b, files.remove(a));
            broken.remove(b);
        }

        public boolean exists(Path p) {
            return files.containsKey(p) || broken.contains(p);
        }
    }

    static CompoundTag data(int marker) {
        var t = new CompoundTag();
        t.put("Players", new CompoundTag());
        t.putInt("Marker", marker);
        return t;
    }

    @Test
    void actualCompressedFilesRecoverLastKnownGoodBackup(@TempDir Path dir) throws Exception {
        var io = new StarRecordIO(StarRecordIO.DISK);
        var file = dir.resolve("records.dat");
        assertTrue(io.write(file, data(1)));
        assertTrue(io.write(file, data(2)));
        Files.writeString(file, "broken");
        var notices = new ArrayList<String>();
        assertEquals(data(1), io.read(file, notices::add));
        assertEquals(List.of("backup_restored"), notices);
        assertEquals(data(1), io.read(file, m -> fail(m)));
    }

    @Test
    void doubleCorruptionLeavesEvidenceBeforeStartingAnEmptyStore(@TempDir Path dir)
            throws Exception {
        var file = dir.resolve("records.dat");
        Files.writeString(file, "main damage");
        Files.writeString(dir.resolve("records.dat.bak"), "backup damage");
        var notices = new ArrayList<String>();
        assertTrue(new StarRecordIO(StarRecordIO.DISK).read(file, notices::add).isEmpty());
        try (var paths = Files.list(dir)) {
            assertEquals(2, paths.filter(p -> p.toString().contains(".corrupt.")).count());
        }
        assertEquals(List.of("records_rebuilt"), notices);
    }

    @Test
    void diskFullRetainsDirtyStateAndNotifiesOnThirdFailure() {
        var fs = new MemoryFiles();
        var io = new StarRecordIO(fs);
        var notices = new ArrayList<String>();
        var store = new StarRecordStore(data(1), io, notices::add);
        var id = UUID.randomUUID();
        store.record(id).reward();
        var before = store.save(new CompoundTag());
        fs.failWrites = true;
        for (int i = 0; i < 3; i++) store.save(new File("records.dat"));
        assertTrue(store.isDirty());
        assertEquals(before, store.save(new CompoundTag()));
        assertEquals(List.of("save_failed"), notices);
        fs.failWrites = false;
        store.save(new File("records.dat"));
        assertFalse(store.isDirty());
        assertEquals(before, io.read(Path.of("records.dat"), m -> fail(m)));
    }

    @Test
    void readOnlyRepairDoesNotDiscardAReadableBackup() {
        var fs = new MemoryFiles();
        var io = new StarRecordIO(fs);
        var file = Path.of("r.dat");
        io.write(file, data(1));
        io.write(file, data(2));
        fs.broken.add(file);
        fs.failCopies = true;
        var messages = new ArrayList<String>();
        assertEquals(data(1), io.read(file, messages::add));
        assertEquals(List.of("backup_restored"), messages);
    }

    @Test
    void backupFailureDoesNotReplaceThePrimary() {
        var fs = new MemoryFiles();
        var io = new StarRecordIO(fs);
        var file = Path.of("r.dat");
        assertTrue(io.write(file, data(1)));
        fs.failCopies = true;
        assertFalse(io.write(file, data(2)));
        assertEquals(data(1), io.read(file, m -> fail(m)));
    }

    @Test
    void inabilityToPreserveEvidenceNeverClearsAnArchive() {
        var fs = new MemoryFiles();
        var file = Path.of("r.dat");
        fs.broken.add(file);
        fs.broken.add(Path.of("r.dat.bak"));
        fs.failCopies = true;
        assertThrows(
                IllegalStateException.class, () -> new StarRecordIO(fs).read(file, m -> fail(m)));
    }
}
