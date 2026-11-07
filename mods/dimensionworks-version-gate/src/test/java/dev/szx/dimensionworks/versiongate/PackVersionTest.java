package dev.szx.dimensionworks.versiongate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackVersionTest {
    @TempDir
    Path directory;

    @Test
    void readsOnlyTheSuppliedGameRootIncludingSpacesAndChinese() throws IOException {
        Path gameRoot = Files.createDirectories(directory.resolve("维度 工坊"));
        Files.writeString(directory.resolve("VERSION"), "9.9.9\n");
        Files.writeString(gameRoot.resolve("VERSION"), "0.3.14\n");
        assertEquals("0.3.14", PackVersion.readVersion(gameRoot).toString());
    }

    @Test
    void acceptsUtf8BomCrLfAndSurroundingAsciiWhitespace() throws IOException {
        Files.writeString(directory.resolve("VERSION"), "\ufeff \t0.3.14\r\n", StandardCharsets.UTF_8);
        assertEquals("0.3.14", PackVersion.readVersion(directory).toString());
    }

    @Test
    void zeroIsARealVersionOnlyWhenWrittenInTheFile() throws IOException {
        Files.writeString(directory.resolve("VERSION"), "0.0.0\n");
        assertEquals(new PackVersion(0, 0, 0), PackVersion.readVersion(directory));
    }

    @Test
    void missingFileNeverUsesParentConfigIndexOrUpdaterState() throws IOException {
        Path gameRoot = Files.createDirectories(directory.resolve("game"));
        Files.writeString(directory.resolve("VERSION"), "9.9.9\n");
        Files.writeString(Files.createDirectories(gameRoot.resolve("config")).resolve("VERSION"), "9.9.9\n");
        Files.writeString(gameRoot.resolve("modrinth.index.json"),
                "{\"name\":\"DimensionWorks\",\"versionId\":\"9.9.9\"}");
        Files.writeString(Files.createDirectories(gameRoot.resolve(".dimensionworks-update")).resolve("state.tsv"),
                "# DimensionWorks update manifest 1\n# version 9.9.9\n");
        IOException error = assertThrows(IOException.class, () -> PackVersion.readVersion(gameRoot));
        assertTrue(error.getMessage().contains(gameRoot.resolve("VERSION").toString()));
    }

    @Test
    void malformedFileReportsItsExactPath() throws IOException {
        Path file = directory.resolve("VERSION");
        for (String value : new String[]{"", "invalid", "v0.3.14", "0.3.14-beta", "0.3", "00.3.14",
                "0.+3.14", "-1.3.14", "0.3.14\n0.3.15", "０.3.14", "2147483648.0.0", "\u00000.3.14"}) {
            Files.writeString(file, value);
            IOException error = assertThrows(IOException.class, () -> PackVersion.readVersion(directory), value);
            assertTrue(error.getMessage().contains(file.toString()), value);
        }
    }

    @Test
    void rejectsNonUtf8OrOversizedFiles() throws IOException {
        Path file = directory.resolve("VERSION");
        Files.write(file, "0.3.14".getBytes(StandardCharsets.UTF_16));
        assertThrows(IOException.class, () -> PackVersion.readVersion(directory));
        Files.write(file, new byte[]{(byte) 0xff, '0', '.', '3', '.', '1', '4'});
        assertThrows(IOException.class, () -> PackVersion.readVersion(directory));
        Files.writeString(file, " ".repeat(65) + "0.3.14");
        assertThrows(IOException.class, () -> PackVersion.readVersion(directory));
    }

    @Test
    void rejectsDirectoryAsVersionFile() throws IOException {
        Files.createDirectory(directory.resolve("VERSION"));
        assertThrows(IOException.class, () -> PackVersion.readVersion(directory));
    }

    @Test
    void parsesAndComparesNumericallyWithoutAnUnknownSentinel() {
        assertTrue(PackVersion.parse("0.3.10").compareTo(PackVersion.parse("0.3.9")) > 0);
        assertTrue(PackVersion.parse("0.2.99").compareTo(PackVersion.parse("0.3.0")) < 0);
        assertEquals(0, PackVersion.parse("0.3.14").compareTo(PackVersion.parse("0.3.14")));
        assertEquals(new PackVersion(Integer.MAX_VALUE, 0, 0), PackVersion.parse("2147483647.0.0"));
        assertThrows(IllegalArgumentException.class, () -> PackVersion.parse(null));
        assertThrows(IllegalArgumentException.class, () -> PackVersion.parse("0.3.14 "));
    }
}
