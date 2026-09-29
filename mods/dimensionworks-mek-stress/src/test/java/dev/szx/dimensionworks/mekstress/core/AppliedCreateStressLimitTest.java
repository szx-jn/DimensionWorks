package dev.szx.dimensionworks.mekstress.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppliedCreateStressLimitTest {

    @Test
    void supportsAtLeastEightStressPerCurrentOutputRpm() {
        assertEquals(16_384.0F, AppliedCreateStressLimit.effectiveGearboxMaximum(4_096.0F, 2_048));
    }

    @Test
    void keepsTheConfiguredMaximumWhenItIsHigher() {
        assertEquals(65_536.0F, AppliedCreateStressLimit.effectiveGearboxMaximum(65_536.0F, 1_024));
    }

    @Test
    void usesTheAbsoluteOutputSpeed() {
        assertEquals(16_384.0F, AppliedCreateStressLimit.effectiveGearboxMaximum(0.0F, -2_048));
    }

    @Test
    void negativeConfiguredMaximumDoesNotCreateNegativeStress() {
        assertEquals(8.0F, AppliedCreateStressLimit.effectiveGearboxMaximum(-1.0F, 1));
    }
}
