package dev.szx.dimensionworks.cavefactory.blockentity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FluidStabilizerQuotaContractTest {
    @Test
    void stabilizerAppliesFinalProductionQuotaBeforeConsumingInput() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/cavefactory/blockentity/FluidStabilizerBlockEntity.java"));

        assertTrue(source.contains("RpmLimitServices.quota().tryConsumeFinalItem("));
        assertTrue(source.contains("owner()"));
        assertTrue(source.indexOf("RpmLimitServices.quota().tryConsumeFinalItem(")
            < source.indexOf("inputTank().drain("));
    }
}
