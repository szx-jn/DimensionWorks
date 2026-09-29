package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorkSchedulerTest {

    @Test
    void spreadsFractionalWorkDeterministically() {
        WorkScheduler scheduler = new WorkScheduler();
        assertEquals(0, scheduler.workCount(0.0D, 0L));
        assertEquals(1, scheduler.workCount(0.5D, 0L));
        assertEquals(0, scheduler.workCount(0.5D, 1L));
        assertEquals(2, WorkScheduler.callsForRate(2.5D, 10L));
        assertEquals(3, WorkScheduler.callsForRate(2.5D, 11L));
    }
}
