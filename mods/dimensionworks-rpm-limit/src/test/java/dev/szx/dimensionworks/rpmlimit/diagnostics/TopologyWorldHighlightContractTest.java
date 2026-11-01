package dev.szx.dimensionworks.rpmlimit.diagnostics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TopologyWorldHighlightContractTest {
    @Test
    void highlightIsClientOnlyAndDrawsAroundTheSelectedBlock() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/diagnostics/TopologyWorldHighlight.java"));

        assertTrue(source.contains("value = Dist.CLIENT"));
        assertTrue(source.contains("Stage.AFTER_PARTICLES"));
        assertTrue(source.contains("new AABB(position).inflate(0.035)"));
        assertTrue(source.contains("LevelRenderer.renderLineBox"));
        assertTrue(source.contains("selectedLevel == minecraft.level"));
        assertTrue(source.contains("minecraft.level.hasChunkAt(position)"));
    }
}
