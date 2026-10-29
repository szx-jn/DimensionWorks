package dev.szx.dimensionworks.versiongate.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VersionMismatchScreenTest {
    @Test
    void windowsUsesNativeExeReleaseAsset() throws IOException {
        assertEquals(
            "update-dimensionworks-client-windows.exe",
            VersionMismatchScreen.updaterFilename("Windows 11", "amd64"));
        assertEquals(
            List.of("C:\\mods\\update-dimensionworks-client-windows.exe", "--release", "v0.2.0"),
            VersionMismatchScreen.updaterCommand(
                Path.of("C:\\mods\\update-dimensionworks-client-windows.exe"), "v0.2.0"));
    }

    @Test
    void linuxUsesNativeAmd64Asset() throws IOException {
        assertEquals(
            "update-dimensionworks-client-linux-amd64",
            VersionMismatchScreen.updaterFilename("Linux", "x86_64"));
    }

    @Test
    void macosUsesNativeArm64Asset() throws IOException {
        assertEquals(
            "update-dimensionworks-client-macos-arm64",
            VersionMismatchScreen.updaterFilename("Mac OS X", "aarch64"));
    }

    @Test
    void unsupportedArchitecturesFailClearly() {
        assertThrows(IOException.class,
            () -> VersionMismatchScreen.updaterFilename("Linux", "aarch64"));
        assertThrows(IOException.class,
            () -> VersionMismatchScreen.updaterFilename("Mac OS X", "x86_64"));
    }
}
