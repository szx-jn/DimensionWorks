package dev.szx.dimensionworks.versiongate.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalUpdaterLauncherTest {
    @TempDir
    Path directory;

    @Test
    void windowsUsesNativeExeReleaseAsset() throws IOException {
        assertEquals(
            "update-dimensionworks-client-windows-amd64.exe",
            LocalUpdaterLauncher.Platform.detect("Windows 11").updaterFilename("amd64"));
    }

    @Test
    void linuxUsesNativeAmd64Asset() throws IOException {
        assertEquals(
            "update-dimensionworks-client-linux-amd64",
            LocalUpdaterLauncher.Platform.detect("Linux").updaterFilename("x86_64"));
    }

    @Test
    void macosUsesNativeArm64Asset() throws IOException {
        assertEquals(
            "update-dimensionworks-client-macos-arm64",
            LocalUpdaterLauncher.Platform.detect("Mac OS X").updaterFilename("aarch64"));
        assertEquals(LocalUpdaterLauncher.Platform.MACOS, LocalUpdaterLauncher.Platform.detect("Darwin"));
    }

    @Test
    void unsupportedArchitecturesFailClearly() {
        assertThrows(IOException.class,
            () -> LocalUpdaterLauncher.Platform.detect("Linux").updaterFilename("aarch64"));
        assertThrows(IOException.class,
            () -> LocalUpdaterLauncher.Platform.detect("Mac OS X").updaterFilename("x86_64"));
    }

    @Test
    void missingLocalUpdaterIsRejected() {
        assertThrows(IOException.class,
                () -> LocalUpdaterLauncher.findUpdater(directory, LocalUpdaterLauncher.Platform.MACOS, "arm64"));
    }

    @Test
    void windowsHandoffWaitsForTheGamePidAndUsesTheRequestedRelease() {
        String script = LocalUpdaterLauncher.windowsHandoff(
                Path.of("C:\\mods\\player's pack"), Path.of("C:\\mods\\updater.exe"),
                "v0.3.8", 12345, Path.of("C:\\mods\\ready"));
        assertTrue(script.contains("Wait-Process -Id 12345 -Timeout 120"));
        assertTrue(script.contains("--release 'v0.3.8'"));
        assertTrue(script.contains("player''s pack"));
    }

    @Test
    void unixHandoffQuotesPathsAndWaitsForOnlyTheGamePid() {
        String script = LocalUpdaterLauncher.unixHandoff(
                Path.of("/tmp/player's pack"), Path.of("/tmp/updater $(marker)"),
                "v0.3.8", 12345, Path.of("/tmp/ready"));
        assertTrue(script.contains("while kill -0 12345"));
        assertTrue(script.contains("cd '/tmp/player'\"'\"'s pack'"));
        assertTrue(script.contains("exec '/tmp/updater $(marker)' --release 'v0.3.8'"));
    }

    @Test
    void terminalFailureDoesNotRequestGameExit() throws Exception {
        Path ready = directory.resolve("failed.ready");
        Path script = directory.resolve("failed.sh");
        Files.writeString(script, "temporary handoff");
        var plan = new LocalUpdaterLauncher.LaunchPlan(
                List.of("/bin/sh", "-c", "exit 7"), directory, ready, script, directory.resolve("failed.log"));
        AtomicBoolean exitRequested = new AtomicBoolean();

        assertThrows(IOException.class,
                () -> LocalUpdaterLauncher.start(plan, () -> exitRequested.set(true), Duration.ofSeconds(2)));
        assertFalse(exitRequested.get());
        assertFalse(Files.exists(ready));
        assertFalse(Files.exists(script));
    }

    @Test
    void terminalReadinessRequestsExitAndCleansHandoffFiles() throws Exception {
        Path ready = directory.resolve("success.ready");
        Path script = directory.resolve("success.sh");
        Files.writeString(script, "temporary handoff");
        var plan = new LocalUpdaterLauncher.LaunchPlan(
                List.of("/bin/sh", "-c", "printf ready > \"$1\"", "handoff", ready.toString()),
                directory, ready, script, directory.resolve("success.log"));
        AtomicBoolean exitRequested = new AtomicBoolean();

        LocalUpdaterLauncher.start(plan, () -> exitRequested.set(true), Duration.ofSeconds(2));
        assertTrue(exitRequested.get());
        assertFalse(Files.exists(ready));
        assertFalse(Files.exists(script));
    }
}
