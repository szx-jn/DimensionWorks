package dev.szx.dimensionworks.mekstress;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ResourceRetirementTest {
    @Test
    void outputCardAndAeBusRouteAreFullyRetired() throws IOException {
        String modSource = readTree(Path.of("src/main/java"));
        String resources = readTree(Path.of("src/main/resources"));
        String mixins = Files.readString(Path.of("src/main/resources/dimensionworks_mek_stress.mixins.json"));
        String startup = Files.readString(Path.of("..", "..", "kubejs", "startup_scripts", "me_memory.js"));
        String server = Files.readString(Path.of("..", "..", "kubejs", "server_scripts", "me_memory.js"));
        String config = Files.readString(Path.of("..", "..", "config", "dimensionworks_mek_stress-common.toml"));
        String defaultConfig = Files.readString(Path.of(
            "..", "..", "defaultconfigs", "dimensionworks_mek_stress-common.toml"));

        assertFalse(modSource.contains("StressOutputCard"), "output card class must be removed");
        assertFalse(modSource.contains("STRESS_OUTPUT_CARD"), "output card registry entry must be removed");
        assertFalse(modSource.contains("MachinePowerRegistry"), "AE machine routing must be removed");
        assertFalse(modSource.contains("ProcessingEnergyRegistry"), "FE workload registry must be removed");
        assertFalse(modSource.contains("SuConversion"), "FE-to-SU conversion must be removed");
        assertFalse(resources.contains("stress_output_card"), "output card resources must be removed");
        assertFalse(config.contains("mekanismPerSu"), "local FE conversion config must be removed");
        assertFalse(defaultConfig.contains("mekanismPerSu"), "default FE conversion config must be removed");
        assertFalse(mixins.contains("ae2.ExportBusPartMixin"), "export bus mixin must be removed");
        assertFalse(mixins.contains("ae2.IOBusPartMixin"), "IO bus mixin must be removed");
        assertFalse(startup.contains("stress_output_card"), "output card must not be in creative tabs");
        assertFalse(server.contains("stress_output_card"), "output card recipe must be removed");
    }

    private static String readTree(Path root) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                if (path.toString().endsWith(".java") || path.toString().endsWith(".json")
                    || path.toString().endsWith(".png")) {
                    builder.append(path).append('\n');
                    if (!path.toString().endsWith(".png")) {
                        builder.append(Files.readString(path)).append('\n');
                    }
                }
            }
        }
        return builder.toString();
    }
}
