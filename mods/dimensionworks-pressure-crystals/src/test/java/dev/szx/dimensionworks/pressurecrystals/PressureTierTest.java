package dev.szx.dimensionworks.pressurecrystals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PressureTierTest {
    @Test
    void belowCompressorMinimumHasNoPressure() {
        assertEquals(PressureTier.NONE, PressureTier.fromRpm(31.99F));
    }

    @Test
    void minimumRunningSpeedIsLowPressure() {
        assertEquals(PressureTier.LOW, PressureTier.fromRpm(32.0F));
    }

    @Test
    void fiveHundredTwelveRpmIsLowPressure() {
        assertEquals(PressureTier.LOW, PressureTier.fromRpm(512.0F));
    }

    @Test
    void aboveFiveHundredTwelveRpmIsMediumPressure() {
        assertEquals(PressureTier.MEDIUM, PressureTier.fromRpm(512.01F));
    }

    @Test
    void twoThousandFortyEightRpmIsMediumPressure() {
        assertEquals(PressureTier.MEDIUM, PressureTier.fromRpm(2048.0F));
    }

    @Test
    void aboveTwoThousandFortyEightRpmIsHighPressure() {
        assertEquals(PressureTier.HIGH, PressureTier.fromRpm(2048.01F));
    }
}
