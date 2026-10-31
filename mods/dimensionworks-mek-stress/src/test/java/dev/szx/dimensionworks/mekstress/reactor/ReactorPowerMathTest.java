package dev.szx.dimensionworks.mekstress.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ReactorPowerMathTest {
    @Test
    void convertsOneFePerTickToTwoSuAcrossFourOutputs() {
        for (int ports = 1; ports <= 4; ports++) {
            float perPort = ReactorPowerMath.stressPerRpm(10_000.0D, ports);
            assertEquals(20_000.0D, perPort * ReactorPowerMath.OUTPUT_RPM * ports, 0.001D);
        }
    }

    @Test
    void noGenerationDoesNotCreateStress() {
        assertEquals(0.0F, ReactorPowerMath.stressPerRpm(0.0D, 4));
        assertEquals(0.0F, ReactorPowerMath.stressPerRpm(Double.NaN, 1));
        assertEquals(0.0F, ReactorPowerMath.stressPerRpm(100.0D, 0));
    }

    @Test
    void onlyGeneratedSaturationCanBeDrained() {
        assertEquals(1_024L, ReactorPowerMath.drainGenerated(1_024.0D, 10_000L));
        assertEquals(10L, ReactorPowerMath.drainGenerated(1_024.0D, 10L));
        assertEquals(0L, ReactorPowerMath.drainGenerated(0.0D, 10L));
        assertEquals(0L, ReactorPowerMath.drainGenerated(Double.NaN, 10L));
    }
}
