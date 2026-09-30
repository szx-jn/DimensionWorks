package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachinePowerMathTest {

    @Test
    void chargesEightSuForEachEffectiveRpm() {
        assertEquals(4_096L, MachinePowerMath.suForRpm(512.0F));
        assertEquals(32_768L, MachinePowerMath.suForRpm(4_096.0F));
        assertEquals(0L, MachinePowerMath.suForRpm(0.0F));
        assertEquals(0L, MachinePowerMath.suForRpm(-512.0F));
    }

    @Test
    void roundsFractionalRpmUpSoRunningMachinesNeverGetFreeTicks() {
        assertEquals(4_097L, MachinePowerMath.suForRpm(512.125F));
    }

    @Test
    void convertsEffectiveRpmScaleIntoPerRpmStress() {
        assertEquals(8.0F, MachinePowerMath.stressPerRpm(1.0F), 1.0E-6F);
        assertEquals(2.0F, MachinePowerMath.stressPerRpm(0.25F), 1.0E-6F);
        assertEquals(0.0F, MachinePowerMath.stressPerRpm(-1.0F), 1.0E-6F);
    }
}
