package dev.szx.dimensionworks.cavefactory.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PhaseCycleTest {

    @Test
    void advancesAfterConfiguredBatchCount() {
        PhaseCycle cycle = new PhaseCycle(FactoryPhase.PHASE_A, 0, 8);

        for (int batch = 0; batch < 7; batch++) {
            cycle.recordSuccess(false);
            assertSame(FactoryPhase.PHASE_A, cycle.phase());
        }

        cycle.recordSuccess(false);

        assertSame(FactoryPhase.PHASE_B, cycle.phase());
        assertEquals(0, cycle.progress());
    }

    @Test
    void redstoneLockKeepsCurrentPhaseAndProgress() {
        PhaseCycle cycle = new PhaseCycle(FactoryPhase.PHASE_A, 7, 8);

        cycle.recordSuccess(true);

        assertSame(FactoryPhase.PHASE_A, cycle.phase());
        assertEquals(7, cycle.progress());
    }

    @Test
    void malformedPersistedProgressIsClamped() {
        PhaseCycle cycle = PhaseCycle.fromState(FactoryPhase.PHASE_B, 99, 8);

        assertEquals(7, cycle.progress());
        assertSame(FactoryPhase.PHASE_B, cycle.phase());
    }
}
