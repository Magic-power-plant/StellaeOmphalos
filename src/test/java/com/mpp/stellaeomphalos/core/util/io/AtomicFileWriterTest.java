package com.mpp.stellaeomphalos.core.util.io;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class AtomicFileWriterTest {
    @TempDir Path temporary;

    @Test
    void writesCompleteContentAndLeavesNoTemporaryFile() throws Exception {
        var target = temporary.resolve("state.dat");
        AtomicFileWriter.write(target, "first".getBytes(StandardCharsets.UTF_8));
        assertEquals("first", Files.readString(target));
        try (var files = Files.list(temporary)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    void failedWriteKeepsPreviousVisibleFile() throws Exception {
        var target = temporary.resolve("state.dat");
        Files.writeString(target, "known-good");
        assertThrows(NullPointerException.class, () -> AtomicFileWriter.write(target, null));
        assertEquals("known-good", Files.readString(target));
        try (var files = Files.list(temporary)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}
