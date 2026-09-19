package com.mpp.stellaeomphalos.core.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLPaths;

public final class GameDirectoryKit {
    private GameDirectoryKit() {}
    public static Path game() { return FMLPaths.GAMEDIR.get(); }
    public static Path config() { return FMLPaths.CONFIGDIR.get(); }
    public static Path ensureDirectory(Path parent, String child) throws IOException {
        var root = parent.toAbsolutePath().normalize();
        var target = root.resolve(child).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Directory escapes parent");
        return Files.createDirectories(target);
    }
}
