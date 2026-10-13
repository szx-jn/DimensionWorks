package dev.szx.dimensionworks.rpmlimit.mixin.ae2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AeMixinContractTest {

    @Test
    void submissionGateRunsAfterSimulationCheck() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/ae2/CraftingServiceMixin.java"));

        assertTrue(source.contains("method = \"submitJob\""));
        assertTrue(source.contains("job.simulation()"));
        assertTrue(source.contains("AeCraftingGate.allowSubmission("));
        assertTrue(source.contains("NO_CPU_FOUND"));
    }

    @Test
    void finalOutputIsGatedBeforeMutableDelivery() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/ae2/CraftingCpuLogicMixin.java"));

        assertTrue(source.contains("method = \"insert\""));
        assertTrue(source.contains("Actionable.MODULATE"));
        assertTrue(source.contains("AeCraftingGate.allowFinalOutput("));
        assertTrue(source.contains("getWaitingFor("));
        assertTrue(!source.contains("job.waitingFor.extract("));
        assertTrue(!source.contains("job.remainingAmount"));
    }

    @Test
    void registersBothAeQuotaMixins() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/resources/dimensionworks_rpm_limit.mixins.json"));

        assertEquals(1, occurrences(source, "\"ae2.CraftingServiceMixin\""));
        assertEquals(1, occurrences(source, "\"ae2.CraftingCpuLogicMixin\""));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
