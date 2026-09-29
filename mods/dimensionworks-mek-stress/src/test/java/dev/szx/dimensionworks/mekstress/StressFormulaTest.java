package dev.szx.dimensionworks.mekstress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StressFormulaTest {

    @Test
    void convertsBetweenJoulesAndStressUnits() {
        assertEquals(2L, StressFormula.joulesToStress(5.0D, 2.5D));
        assertEquals(1L, StressFormula.joulesToStress(0.1D, 2.5D));
        assertEquals(5.0D, StressFormula.stressToJoules(2L, 2.5D), 0.000_001D);
        assertEquals(0L, StressFormula.joulesToStress(0.0D, 2.5D));
        assertEquals(0.0D, StressFormula.stressToJoules(0L, 2.5D), 0.000_001D);
    }

    @Test
    void floorsStoredJoulesWhenDisplayingStressUnits() {
        assertEquals(4L, StressFormula.joulesToStressFloor(10.0D, 2.5D));
        assertEquals(0L, StressFormula.joulesToStressFloor(2.4D, 2.5D));
        assertEquals(0L, StressFormula.joulesToStressFloor(-1.0D, 2.5D));
    }

    @Test
    void usesStepOverspeedCurveAtExpectedBreakpoints() {
        assertEquals(1.0D, StressFormula.multiplier(512, 512, 15), 0.000_001D);
        assertEquals(2.0D, StressFormula.multiplier(1024, 512, 15), 0.000_001D);
        assertEquals(2.0D, StressFormula.multiplier(1536, 512, 15), 0.000_001D);
        assertEquals(2.5D, StressFormula.multiplier(2048, 512, 15), 0.000_001D);
        assertEquals(3.5D, StressFormula.multiplier(4096, 512, 15), 0.000_001D);
        assertEquals(10.0D, StressFormula.multiplier(8192, 512, 15), 0.000_001D);
        assertEquals(15.0D, StressFormula.multiplier(10240, 512, 15), 0.000_001D);
    }

    @Test
    void clampsRpmAndStressToSupportedBounds() {
        assertEquals(0, StressFormula.clampRpm(-1, 4096, 10240));
        assertEquals(2048, StressFormula.clampRpm(2048, 4096, 10240));
        assertEquals(4096, StressFormula.clampRpm(8192, 4096, 10240));
        assertEquals(10240, StressFormula.clampRpm(99999, 99999, 10240));
        assertEquals(0L, StressFormula.clampStress(-1L, 100L));
        assertEquals(100L, StressFormula.clampStress(101L, 100L));
    }

    @Test
    void scalesNativeTicksByTheOverspeedMultiplier() {
        assertEquals(100, StressFormula.effectiveTicks(100, 1.0D));
        assertEquals(50, StressFormula.effectiveTicks(100, 2.0D));
        assertEquals(40, StressFormula.effectiveTicks(100, 2.5D));
        assertEquals(1, StressFormula.effectiveTicks(1, 15.0D));
    }
}
