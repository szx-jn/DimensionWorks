package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StressTransferMathTest {
    @Test void wholeSuInputMatchesCreateFloatMultiplicationAndRoundsDown() {
        assertEquals(50, StressTransferMath.paidSu(0.5F, 100));
        assertEquals(50, StressTransferMath.paidSu(0.5F, 101));
        assertEquals(0, StressTransferMath.paidSu(0.01F, 50));
        assertEquals(Long.MAX_VALUE, StressTransferMath.paidSu(Float.MAX_VALUE, Float.MAX_VALUE));
    }

    @Test void outputFloatCapacityNeverExceedsItsPaidSuBudget() {
        long[] budgets = {1, 2, 3, 10, 50, 1_000, Long.MAX_VALUE};
        float[] rpms = {0.25F, 0.7F, 1, 3, 32, 256, 10_240};
        for (long budget : budgets) for (float rpm : rpms) {
            float capacity = StressTransferMath.capacityFor(budget, rpm);
            float used = capacity * rpm;
            assertTrue(Float.isFinite(used) && used < 0x1.0p63f && Math.ceil(used) <= budget,
                () -> "Output exceeds " + budget + " SU at " + rpm + " RPM: " + capacity + " -> " + used);
        }
    }
}
