package dev.szx.dimensionworks.mekstress.memory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MemoryGridServiceContractTest {
    @Test
    void driveAndGearboxScansOnlyUseActiveGridNodes() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MemoryGridService.java"));
        long activeChecks = source.lines()
            .filter(line -> line.contains("node.isActive()"))
            .count();

        assertTrue(activeChecks >= 2L, "both node scans must ignore offline or channel-less nodes");
    }

    @Test
    void driveScanSkipsNodesWithoutAWorld() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/memory/MemoryGridService.java"));

        assertTrue(source.contains("drive.getLevel() == null"));
    }
}
