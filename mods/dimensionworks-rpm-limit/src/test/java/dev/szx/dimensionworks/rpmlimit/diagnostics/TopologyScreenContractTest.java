package dev.szx.dimensionworks.rpmlimit.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TopologyScreenContractTest {

    @Test
    void layoutAndRenderingShareTheVirtualInclusiveIndex() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/rpmlimit/diagnostics/TopologyScreen.java"));

        assertEquals(2, occurrences(source, "TopologyEdges.indices(snapshot.nodes())"));
        assertFalse(source.contains("if (!snapshot.nodes().get(i).virtual())"));
        assertTrue(source.contains("drawRightArrow(g, (x1 + x2) / 2, y1"));
        assertFalse(source.contains("g.drawString(font, \"v\""));
        assertTrue(source.contains("Component.literal(\"拆除\")"));
        assertTrue(source.contains("Component.literal(\"传送到此处\")"));
        assertTrue(source.contains("Component.literal(\"高亮显示\")"));
        assertTrue(source.contains("new ConfirmScreen("));
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
