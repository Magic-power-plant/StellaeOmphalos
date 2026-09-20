package com.mpp.stellaeomphalos.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;

class ShardProcessTest {
    @Test
    void differentJvmsAndLanguagesResolveTheSameSeeds() throws Exception {
        assertEquals(run("en-US"), run("zh-CN"));
    }

    private static List<String> run(String locale) throws Exception {
        String javaExe =
                Path.of(
                                System.getProperty("java.home"),
                                "bin",
                                "java"
                                        + (System.getProperty("os.name").startsWith("Windows")
                                                ? ".exe"
                                                : ""))
                        .toString();
        var process =
                new ProcessBuilder(
                                javaExe,
                                "-cp",
                                System.getProperty("stellaeomphalos.testClasspath"),
                                ShardDeterminismProbe.class.getName(),
                                locale)
                        .redirectErrorStream(true)
                        .start();
        var output =
                new String(
                        process.getInputStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(0, process.exitValue(), output);
        var lines = output.lines().filter(s -> s.startsWith("SHARD=")).toList();
        assertEquals(32, lines.size(), output);
        return lines;
    }
}
