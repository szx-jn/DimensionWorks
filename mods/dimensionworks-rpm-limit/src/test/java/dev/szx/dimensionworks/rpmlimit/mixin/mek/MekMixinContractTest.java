package dev.szx.dimensionworks.rpmlimit.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

class MekMixinContractTest {

    @Test
    void wrapsTheServerTickWithContextAndClearsIt() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/mek/TileEntityMekanismMixin.java"));

        assertEquals(2, occurrences(source, "method = \"tickServer\""));
        assertEquals(1, occurrences(source, "at = @At(\"HEAD\")"));
        assertEquals(1, occurrences(source, "at = @At(\"RETURN\")"));
        assertTrue(source.contains("MekProcessingContext.push("));
        assertTrue(source.contains("MekProcessingContext.pop("));
    }

    @Test
    void gatesTheActualCachedRecipeOperationCount() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/mixin/mek/CachedRecipeMixin.java"));

        assertEquals(1, occurrences(source, "@Redirect"));
        assertTrue(source.contains("method = \"process\""));
        assertTrue(source.contains("OperationTracker;currentMax:I"));
        assertTrue(source.contains("MekProcessingGate.allowOperations("));
    }

    @Test
    void registersBothMekQuotaMixins() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/resources/dimensionworks_rpm_limit.mixins.json"));

        assertNotNull(source);
        assertTrue(source.contains("\"mek.TileEntityMekanismMixin\""));
        assertTrue(source.contains("\"mek.CachedRecipeMixin\""));
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
