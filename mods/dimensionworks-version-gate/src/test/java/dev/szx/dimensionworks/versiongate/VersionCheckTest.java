package dev.szx.dimensionworks.versiongate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersionCheckTest {
    @TempDir
    Path directory;

    @Test
    void comparesTheRootVersionForOlderEqualAndNewerClients() throws IOException {
        for (String[] versions : new String[][]{
                {"0.3.14", "0.3.15", "TOO_OLD"},
                {"0.3.15", "0.3.15", "CURRENT"},
                {"0.3.16", "0.3.15", "CURRENT"},
                {"0.3.10", "0.3.9", "CURRENT"},
                {"0.0.0", "0.0.1", "TOO_OLD"}}) {
            Files.writeString(directory.resolve("VERSION"), versions[0] + "\n");
            VersionCheck check = VersionCheck.evaluate(directory, versions[1]);
            assertEquals(VersionCheck.Status.valueOf(versions[2]), check.status());
            assertEquals(versions[0], check.localText());
            assertEquals(versions[1], check.serverText());
        }
    }

    @Test
    void rereadsVersionAtEveryLoginInsteadOfCachingIt() throws IOException {
        Files.writeString(directory.resolve("VERSION"), "0.3.14\n");
        assertEquals(VersionCheck.Status.TOO_OLD, VersionCheck.evaluate(directory, "0.3.15").status());
        Files.writeString(directory.resolve("VERSION"), "0.3.15\n");
        assertEquals(VersionCheck.Status.CURRENT, VersionCheck.evaluate(directory, "0.3.15").status());
    }

    @Test
    void unreadableLocalVersionIsAnErrorAndNeverDisplaysZero() {
        VersionCheck check = VersionCheck.evaluate(directory, "0.3.15");
        assertEquals(VersionCheck.Status.LOCAL_VERSION_ERROR, check.status());
        assertNull(check.local());
        assertEquals("无法读取", check.localText());
        assertEquals("0.3.15", check.serverText());
    }

    @Test
    void invalidServerVersionKeepsTheRealLocalVersionAndDisablesUpdating() throws IOException {
        Files.writeString(directory.resolve("VERSION"), "0.3.14\n");
        for (String server : new String[]{"", "invalid", "v0.3.15", "2147483648.0.0"}) {
            VersionCheck check = VersionCheck.evaluate(directory, server);
            assertEquals(VersionCheck.Status.SERVER_VERSION_ERROR, check.status());
            assertEquals("0.3.14", check.localText());
            assertNull(check.server());
        }
    }
}
