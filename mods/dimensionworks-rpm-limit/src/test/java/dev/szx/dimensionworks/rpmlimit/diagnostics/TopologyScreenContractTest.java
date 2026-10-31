package dev.szx.dimensionworks.rpmlimit.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
