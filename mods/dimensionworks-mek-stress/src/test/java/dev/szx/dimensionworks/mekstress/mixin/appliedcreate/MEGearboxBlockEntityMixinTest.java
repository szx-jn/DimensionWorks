package dev.szx.dimensionworks.mekstress.mixin.appliedcreate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MEGearboxBlockEntityMixinTest {
    @Test
    void exportRequiresAnActiveGridNode() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/mekstress/mixin/appliedcreate/MEGearboxBlockEntityMixin.java"));

        assertTrue(source.contains("!self.getMainNode().isActive()"));
    }
}
