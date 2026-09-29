package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicOutputPlannerTest {

    @Test
    void requiresEveryOutputToFit() {
        assertTrue(AtomicOutputPlanner.canFit(16, 16, 64));
        assertTrue(AtomicOutputPlanner.allFit(32, 64));
        assertFalse(AtomicOutputPlanner.allFit(64, 32));
    }

    @Test
    void exactCapacityFits() {
        assertTrue(AtomicOutputPlanner.canFit(64, 0, 64));
        assertFalse(AtomicOutputPlanner.canFit(65, 0, 64));
    }

    @Test
    void clampsNegativeValues() {
        assertTrue(AtomicOutputPlanner.canFit(-5, 0, 64));
    }
}
