package dev.szx.dimensionworks.pressurecrystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PressureTierTest {
    @Test
    void stoppedCompressorHasNoPressure() {
        assertEquals(PressureTier.NONE, PressureTier.fromRpm(0.0F));
    }

    @Test
    void anyPositiveSpeedUpTo512IsLowPressure() {
        assertEquals(PressureTier.LOW, PressureTier.fromRpm(1.0F));
        assertEquals(PressureTier.LOW, PressureTier.fromRpm(32.0F));
        assertEquals(PressureTier.LOW, PressureTier.fromRpm(512.0F));
    }

    @Test
    void above512Through2048IsMediumPressure() {
        assertEquals(PressureTier.MEDIUM, PressureTier.fromRpm(512.01F));
        assertEquals(PressureTier.MEDIUM, PressureTier.fromRpm(2048.0F));
    }

    @Test
    void above2048IsHighPressure() {
        assertEquals(PressureTier.HIGH, PressureTier.fromRpm(2048.01F));
        assertEquals(PressureTier.HIGH, PressureTier.fromRpm(4096.0F));
    }
}
