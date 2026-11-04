package dev.szx.dimensionworks.stressgaugealarm.mixin;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StressGaugeMixinContractTest {
    @Test
    void onlyNotifiesNeighborsAfterTheAnalogOutputChanges() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/szx/dimensionworks/stressgaugealarm/mixin/StressGaugeBlockEntityMixin.java"));
        assertTrue(source.contains("dimensionworks$oldSignal != StressGaugeSignal.analogOutput(self.dialTarget)"));
        assertTrue(source.contains("dimensionworks$updateDepth == 0"));
    }
}
