package com.mpp.stellaeomphalos.player.progress;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.*;

import java.io.*;
import java.nio.file.*;
import java.util.function.Consumer;

/** Main-thread, atomic aggregate writes with an explicit last-known-good backup. */
public final class StarRecordIO {
    public interface Filesystem {
        CompoundTag read(Path path) throws IOException;

        void write(Path path, CompoundTag tag) throws IOException;

        void copy(Path from, Path to) throws IOException;

        void replace(Path from, Path to) throws IOException;

        boolean exists(Path path);
    }

    public static final Filesystem DISK =
            new Filesystem() {
                public CompoundTag read(Path path) throws IOException {
                    return NbtIo.readCompressed(path.toFile());
                }

                public void write(Path path, CompoundTag tag) throws IOException {
                    NbtIo.writeCompressed(tag, path.toFile());
                }

                public void copy(Path from, Path to) throws IOException {
                    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                }

                public void replace(Path from, Path to) throws IOException {
                    try {
                        Files.move(
                                from,
                                to,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                public boolean exists(Path path) {
                    return Files.exists(path);
                }
            };
    private final Filesystem fs;

    public StarRecordIO(Filesystem fs) {
        this.fs = fs;
    }

    private static Path sibling(Path file, String suffix) {
        return file.resolveSibling(file.getFileName() + suffix);
    }

    public CompoundTag read(Path file, Consumer<String> notice) {
        var backup = sibling(file, ".bak");
        if (!fs.exists(file) && !fs.exists(backup)) return new CompoundTag();
        try {
            return checked(fs.read(file));
        } catch (IOException | RuntimeException primary) {
            try {
                var restored = checked(fs.read(backup));
                try {
                    fs.copy(backup, file);
                } catch (IOException restore) {
                    LogUtils.getLogger()
                            .error(
                                    "Backup loaded but primary repair failed {}; retaining restored"
                                        + " memory",
                                    file,
                                    restore);
                }
                notice.accept("backup_restored");
                return restored;
            } catch (IOException | RuntimeException secondary) {
                String stamp = ".corrupt." + System.currentTimeMillis();
                try {
                    if (fs.exists(file)) fs.copy(file, sibling(file, stamp));
                    if (fs.exists(backup)) fs.copy(backup, sibling(backup, stamp));
                } catch (IOException evidence) {
                    throw new IllegalStateException(
                            "Cannot preserve corrupt star records", evidence);
                }
                LogUtils.getLogger().error("Star records and backup unreadable: {}", file, primary);
                notice.accept("records_rebuilt");
                return new CompoundTag();
            }
        }
    }

    private static CompoundTag checked(CompoundTag tag) throws IOException {
        if (tag == null
                || !tag.contains("data", Tag.TAG_COMPOUND)
                || !tag.getCompound("data").contains("Players", Tag.TAG_COMPOUND))
            throw new IOException("Not a star-record aggregate");
        return tag.getCompound("data");
    }

    public boolean write(Path file, CompoundTag data) {
        var temporary = sibling(file, ".tmp");
        try {
            var root = new CompoundTag();
            root.put("data", data.copy());
            fs.write(temporary, root);
            if (fs.exists(file)) {
                boolean valid = false;
                try {
                    checked(fs.read(file));
                    valid = true;
                } catch (IOException invalid) {
                    /* Never overwrite the good backup with a corrupt primary. */
                }
                if (valid) fs.copy(file, sibling(file, ".bak"));
            }
            fs.replace(temporary, file);
            return true;
        } catch (IOException | RuntimeException e) {
            LogUtils.getLogger()
                    .error("Unable to save star records {}; retaining dirty memory state", file, e);
            return false;
        }
    }
}
