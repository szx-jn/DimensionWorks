package dev.szx.dimensionworks.mekstress.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorkSchedulerTest {

    @Test
    void spreadsFractionalWorkDeterministically() {
        WorkScheduler scheduler = new WorkScheduler();
        assertEquals(0, scheduler.workCount(0.0D, 0L));
        assertEquals(0, scheduler.workCount(0.5D, 0L));
        assertEquals(1, scheduler.workCount(0.5D, 1L));
        assertEquals(2, WorkScheduler.callsForRate(2.5D, 10L));
        assertEquals(3, WorkScheduler.callsForRate(2.5D, 11L));
    }

    @Test
    void aTickGapDoesNotCatchUpHistoricalWork() {
        WorkScheduler.Advance first = WorkScheduler.advance(1.0D, 0L, null);
        WorkScheduler.Advance afterGap = WorkScheduler.advance(1.0D, 12_000L, first);

        assertEquals(1, afterGap.calls());
    }

    @Test
    void aRateChangeDoesNotCatchUpHistoricalWork() {
        WorkScheduler.Advance first = WorkScheduler.advance(1.0D, 100L, null);
        WorkScheduler.Advance changed = WorkScheduler.advance(10.0D, 101L, first);

        assertEquals(10, changed.calls());
    }

    @Test
    void contiguousFractionalWorkStillAveragesOut() {
        WorkScheduler.Advance previous = null;
        long total = 0L;
        for (long tick = 0L; tick < 1_000L; tick++) {
            previous = WorkScheduler.advance(2.5D, tick, previous);
            total += previous.calls();
        }

        assertEquals(2_500L, total);
    }

    @Test
    void repeatedReloadAndRateChangesCannotBorrowUnpaidFractionalWork() {
        for (long tick=0; tick<1000; tick++) {
            assertEquals(0, WorkScheduler.advance(0.25D,tick,null).calls());
        }
        WorkScheduler.Advance previous=null;
        int total=0;
        for(long tick=0;tick<32;tick++) {
            previous=WorkScheduler.advance(1.0D/32,tick,previous);
            total+=previous.calls();
            assertEquals(tick==31?1:0,total);
        }
    }
}
